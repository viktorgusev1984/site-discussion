package com.example.discussions;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.discussions.security.AuthErrorWriter;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.InsufficientAuthenticationException;

class AuthErrorWriterTest {
  private final AuthErrorWriter writer = new AuthErrorWriter(new ObjectMapper().findAndRegisterModules());

  @Test void explainsAnAnonymousRequestWith401InsteadOfAnEmptyBody() throws Exception {
    var response = new MockHttpServletResponse();

    writer.commence(new MockHttpServletRequest("POST", "/api/ai/discussions/1/summary"), response,
        new InsufficientAuthenticationException("Anonymous user"));

    assertThat(response.getStatus()).isEqualTo(401);
    assertThat(response.getContentType()).startsWith("application/json");
    assertThat(response.getContentAsString(StandardCharsets.UTF_8))
        .contains("\"status\":401")
        .contains("Сессия истекла. Войдите снова");
  }

  @Test void explainsADeniedRequestWith403AndTheSameBodyShape() throws Exception {
    var response = new MockHttpServletResponse();

    writer.handle(new MockHttpServletRequest("PUT", "/api/discussions/1"), response,
        new AccessDeniedException("Not the author"));

    assertThat(response.getStatus()).isEqualTo(403);
    assertThat(response.getContentAsString(StandardCharsets.UTF_8))
        .contains("\"status\":403")
        .contains("Недостаточно прав");
  }
}
