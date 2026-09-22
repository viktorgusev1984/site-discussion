package com.example.discussions.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

/**
 * Renders authentication failures in the same JSON shape as {@code GlobalExceptionHandler}.
 * Without it Spring answers an expired token with an empty 403, which the SPA cannot explain to the user.
 */
@Component
public class AuthErrorWriter implements AuthenticationEntryPoint, AccessDeniedHandler {
  private final ObjectMapper json;

  public AuthErrorWriter(ObjectMapper json) {
    this.json = json;
  }

  @Override
  public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException reason)
      throws IOException {
    write(response, HttpStatus.UNAUTHORIZED, "Сессия истекла. Войдите снова");
  }

  @Override
  public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException reason)
      throws IOException {
    write(response, HttpStatus.FORBIDDEN, "Недостаточно прав");
  }

  private void write(HttpServletResponse response, HttpStatus status, String message) throws IOException {
    response.setStatus(status.value());
    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
    response.setCharacterEncoding(StandardCharsets.UTF_8.name());
    json.writeValue(response.getOutputStream(),
        Map.of("timestamp", Instant.now(), "status", status.value(), "error", message, "fields", Map.of()));
  }
}
