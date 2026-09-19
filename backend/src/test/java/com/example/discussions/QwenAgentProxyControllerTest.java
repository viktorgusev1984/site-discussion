package com.example.discussions;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.example.discussions.controller.QwenAgentProxyController;
import com.example.discussions.exception.ApiException;
import com.example.discussions.model.User;
import com.example.discussions.repository.AiChatSessionRepository;
import com.example.discussions.service.CurrentUser;
import java.net.URI;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;

class QwenAgentProxyControllerTest {
  @Test void refusesAChatSessionOwnedByAnotherUserBeforeContactingTheDaemon() {
    var sessions = mock(AiChatSessionRepository.class);
    var current = mock(CurrentUser.class);
    var user = new User(); user.id = 7L;
    when(current.required()).thenReturn(user);
    String sessionId = "550e8400-e29b-41d4-a716-446655440000";
    when(sessions.existsBySessionIdAndUser(sessionId, user)).thenReturn(false);
    var request = new MockHttpServletRequest("POST", "/api/agent/session/" + sessionId + "/prompt");
    request.setRequestURI("/api/agent/session/" + sessionId + "/prompt");

    var controller = new QwenAgentProxyController(sessions, current, URI.create("http://127.0.0.1:1"), "secret");
    assertThatThrownBy(() -> controller.proxy(request)).isInstanceOf(ApiException.class)
        .hasMessage("Сессия AI-помощника не найдена");
  }

  @Test void translatesDaemonConnectionFailureIntoAStableApiError() {
    var sessions = mock(AiChatSessionRepository.class);
    var current = mock(CurrentUser.class);
    when(current.required()).thenReturn(new User());
    var request = new MockHttpServletRequest("GET", "/api/agent/capabilities");
    request.setRequestURI("/api/agent/capabilities");

    var controller = new QwenAgentProxyController(sessions, current, URI.create("http://127.0.0.1:1"), "secret");
    assertThatThrownBy(() -> controller.proxy(request))
        .isInstanceOfSatisfying(ApiException.class, error ->
            org.assertj.core.api.Assertions.assertThat(error.status).isEqualTo(HttpStatus.BAD_GATEWAY))
        .hasMessage("AI-помощник временно недоступен");
  }

  @Test void reportsAnUnconfiguredDaemonWithoutOpeningAConnection() {
    var sessions = mock(AiChatSessionRepository.class);
    var current = mock(CurrentUser.class);
    when(current.required()).thenReturn(new User());
    var request = new MockHttpServletRequest("GET", "/api/agent/capabilities");
    request.setRequestURI("/api/agent/capabilities");

    var controller = new QwenAgentProxyController(sessions, current, URI.create("http://127.0.0.1:1"), " ");
    assertThatThrownBy(() -> controller.proxy(request))
        .isInstanceOfSatisfying(ApiException.class, error ->
            org.assertj.core.api.Assertions.assertThat(error.status).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE))
        .hasMessage("AI-помощник не настроен");
  }
}
