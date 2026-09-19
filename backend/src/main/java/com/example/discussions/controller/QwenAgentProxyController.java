package com.example.discussions.controller;

import com.example.discussions.exception.ApiException;
import com.example.discussions.repository.AiChatSessionRepository;
import com.example.discussions.service.CurrentUser;
import jakarta.servlet.http.HttpServletRequest;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Set;
import java.util.regex.Pattern;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

/** Authenticated, ownership-aware bridge used by the embedded Qwen Web Shell. */
@RestController
public class QwenAgentProxyController {
  private static final String PREFIX = "/api/agent";
  private static final Pattern SESSION_PATH = Pattern.compile("^/session/([0-9a-fA-F-]{36})(?:/.*)?$");
  private static final Set<String> FORWARDED_REQUEST_HEADERS = Set.of(
      "accept", "content-type", "last-event-id", "x-qwen-event-epoch", "x-qwen-client-id");
  private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
  private final AiChatSessionRepository sessions;
  private final CurrentUser current;
  private final URI daemon;
  private final String daemonToken;

  public QwenAgentProxyController(AiChatSessionRepository sessions, CurrentUser current,
      @Value("${app.qwen-agent.url:http://localhost:4170}") URI daemon,
      @Value("${app.qwen-agent.token:}") String daemonToken) {
    this.sessions = sessions;
    this.current = current;
    this.daemon = daemon;
    this.daemonToken = daemonToken;
  }

  @RequestMapping("/api/agent/**")
  public ResponseEntity<StreamingResponseBody> proxy(HttpServletRequest incoming) throws Exception {
    var user = current.required();
    String path = incoming.getRequestURI().substring(PREFIX.length());
    if (path.isBlank()) path = "/capabilities";
    var matcher = SESSION_PATH.matcher(path);
    boolean publicCapability = path.equals("/capabilities") && incoming.getMethod().equals("GET");
    if (!publicCapability && (!matcher.matches() || !sessions.existsBySessionIdAndUser(matcher.group(1), user))) {
      throw ApiException.notFound("Сессия AI-помощника не найдена");
    }

    String query = incoming.getQueryString();
    URI target = daemon.resolve(path + (query == null ? "" : "?" + query));
    byte[] body = incoming.getInputStream().readAllBytes();
    var builder = HttpRequest.newBuilder(target).timeout(Duration.ofMinutes(3))
        .header("Authorization", "Bearer " + daemonToken)
        .method(incoming.getMethod(), body.length == 0
            ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofByteArray(body));
    incoming.getHeaderNames().asIterator().forEachRemaining(name -> {
      if (FORWARDED_REQUEST_HEADERS.contains(name.toLowerCase())) builder.header(name, incoming.getHeader(name));
    });
    HttpResponse<InputStream> response = http.send(builder.build(), HttpResponse.BodyHandlers.ofInputStream());
    var headers = new HttpHeaders();
    response.headers().firstValue("content-type").ifPresent(value -> headers.set("Content-Type", value));
    response.headers().firstValue("x-qwen-event-epoch").ifPresent(value -> headers.set("X-Qwen-Event-Epoch", value));
    response.headers().firstValue("retry-after").ifPresent(value -> headers.set("Retry-After", value));
    StreamingResponseBody stream = output -> {
      try (InputStream input = response.body()) { input.transferTo(output); }
    };
    return new ResponseEntity<>(stream, headers, HttpStatusCode.valueOf(response.statusCode()));
  }
}
