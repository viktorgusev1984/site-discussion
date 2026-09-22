package com.example.discussions.service.ai;

import com.example.discussions.exception.ApiException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

/** Adapter for the Qwen Code daemon. The agent loop remains outside the Spring process. */
@Service
public class QwenAgentClient {
  private static final Logger log = LoggerFactory.getLogger(QwenAgentClient.class);
  private final HttpClient http;
  private final ObjectMapper json;
  private final URI endpoint;
  private final String token;
  private final Duration timeout;

  public QwenAgentClient(ObjectMapper json,
      @Value("${app.qwen-agent.url:http://localhost:4170}") URI endpoint,
      @Value("${app.qwen-agent.token:}") String token,
      @Value("${app.qwen-agent.timeout:PT2M}") Duration timeout) {
    this.json = json;
    this.endpoint = endpoint;
    this.token = token;
    this.timeout = timeout;
    // The daemon drops any request carrying an Upgrade header, so the default h2c handshake never reaches it.
    this.http = HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1)
        .connectTimeout(Duration.ofSeconds(5)).build();
  }

  public String run(String prompt) {
    if (token.isBlank()) throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "AI-помощник не настроен");
    try {
      JsonNode session = post("/session", Map.of("sessionScope", "thread"));
      String sessionId = session.path("sessionId").asText();
      JsonNode admitted = post("/session/" + sessionId + "/prompt",
          Map.of("prompt", List.of(Map.of("type", "text", "text", prompt)), "eventDetailMode", "summary"));
      String promptId = admitted.path("promptId").asText();
      if (sessionId.isBlank() || promptId.isBlank()) throw new IllegalStateException("Missing Qwen session identity");

      Instant deadline = Instant.now().plus(timeout);
      while (Instant.now().isBefore(deadline)) {
        JsonNode turn = get("/session/" + sessionId + "/turns/" + promptId);
        String state = turn.path("state").asText();
        if ("error".equals(state) || "failed".equals(state) || "cancelled".equals(state)) {
          log.warn("Qwen turn {} in session {} ended as {}: {}", promptId, sessionId, state, turn.path("error"));
          throw new ApiException(HttpStatus.BAD_GATEWAY, "AI-помощник не смог выполнить запрос");
        }
        if ("completed".equals(state) || turn.hasNonNull("resultText")) {
          String result = turn.path("resultText").asText().trim();
          if (result.isBlank()) throw new IllegalStateException("Qwen returned an empty result");
          return result;
        }
        Thread.sleep(250);
      }
      throw new ApiException(HttpStatus.GATEWAY_TIMEOUT, "AI-помощник не ответил вовремя");
    } catch (ApiException e) {
      throw e;
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "Запрос к AI-помощнику прерван");
    } catch (Exception e) {
      log.warn("Qwen prompt call failed", e);
      throw new ApiException(HttpStatus.BAD_GATEWAY, "AI-помощник временно недоступен");
    }
  }

  public String createSession() {
    if (token.isBlank()) throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "AI-помощник не настроен");
    try {
      String sessionId = post("/session", Map.of("sessionScope", "thread")).path("sessionId").asText();
      if (sessionId.isBlank()) throw new IllegalStateException("Missing Qwen session identity");
      return sessionId;
    } catch (Exception e) {
      log.warn("Qwen session creation failed", e);
      throw new ApiException(HttpStatus.BAD_GATEWAY, "AI-помощник временно недоступен");
    }
  }

  private JsonNode get(String path) throws Exception {
    return send(HttpRequest.newBuilder(endpoint.resolve(path)).GET());
  }

  private JsonNode post(String path, Object body) throws Exception {
    return send(HttpRequest.newBuilder(endpoint.resolve(path))
        .header("Content-Type", "application/json")
        .POST(HttpRequest.BodyPublishers.ofString(json.writeValueAsString(body))));
  }

  private JsonNode send(HttpRequest.Builder builder) throws Exception {
    HttpRequest request = builder.timeout(timeout).header("Authorization", "Bearer " + token).build();
    HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
    if (response.statusCode() < 200 || response.statusCode() >= 300) {
      throw new IllegalStateException("Qwen daemon returned HTTP " + response.statusCode());
    }
    return json.readTree(response.body());
  }
}
