package com.example.discussions;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.discussions.exception.ApiException;
import com.example.discussions.service.ai.QwenAgentClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

class QwenAgentClientTest {
  private HttpServer server;
  private final Set<String> upgradeHeaders = ConcurrentHashMap.newKeySet();

  @AfterEach void stopsTheStubDaemon() {
    if (server != null) server.stop(0);
  }

  /** Stubs the daemon turn endpoint; the daemon reports progress in "state", not "status". */
  private QwenAgentClient clientWithTurns(List<String> turnResponses) throws Exception {
    server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    AtomicInteger polls = new AtomicInteger();
    server.createContext("/session", exchange -> {
      String upgrade = exchange.getRequestHeaders().getFirst("Upgrade");
      if (upgrade != null) upgradeHeaders.add(upgrade);
      String path = exchange.getRequestURI().getPath();
      String body;
      if (path.contains("/turns/")) {
        body = turnResponses.get(Math.min(polls.getAndIncrement(), turnResponses.size() - 1));
      } else if (path.endsWith("/prompt")) {
        body = "{\"promptId\":\"p-1\"}";
      } else {
        body = "{\"sessionId\":\"s-1\"}";
      }
      byte[] payload = body.getBytes(StandardCharsets.UTF_8);
      exchange.getResponseHeaders().set("Content-Type", "application/json");
      exchange.sendResponseHeaders(200, payload.length);
      try (OutputStream out = exchange.getResponseBody()) { out.write(payload); }
    });
    server.start();
    return new QwenAgentClient(new ObjectMapper(),
        URI.create("http://127.0.0.1:" + server.getAddress().getPort()), "secret", Duration.ofSeconds(10));
  }

  @Test void waitsOutARunningTurnAndReturnsItsResultText() throws Exception {
    var client = clientWithTurns(List.of(
        "{\"sessionId\":\"s-1\",\"promptId\":\"p-1\",\"state\":\"running\"}",
        "{\"sessionId\":\"s-1\",\"promptId\":\"p-1\",\"state\":\"completed\",\"resultText\":\"OK\"}"));

    assertThat(client.run("Reply with exactly: OK")).isEqualTo("OK");
  }

  @Test void reportsAFailedTurnAtOnceInsteadOfPollingUntilTheDeadline() throws Exception {
    var client = clientWithTurns(List.of(
        "{\"sessionId\":\"s-1\",\"promptId\":\"p-1\",\"state\":\"error\","
            + "\"error\":{\"message\":\"Internal error\",\"code\":\"-32603\"}}"));

    long started = System.nanoTime();
    assertThatThrownBy(() -> client.run("Reply with exactly: OK"))
        .isInstanceOfSatisfying(ApiException.class,
            error -> assertThat(error.status).isEqualTo(HttpStatus.BAD_GATEWAY));
    assertThat(Duration.ofNanos(System.nanoTime() - started)).isLessThan(Duration.ofSeconds(5));
  }

  @Test void neverSendsTheH2cUpgradeTheDaemonWouldDrop() throws Exception {
    var client = clientWithTurns(List.of(
        "{\"sessionId\":\"s-1\",\"promptId\":\"p-1\",\"state\":\"completed\",\"resultText\":\"OK\"}"));

    client.run("Reply with exactly: OK");

    assertThat(upgradeHeaders).isEmpty();
  }
}
