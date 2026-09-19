package com.example.discussions;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.example.discussions.dto.NotificationChannelDtos.CreateRequest;
import com.example.discussions.model.NotificationChannelType;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

class NotificationChannelDtosValidationTest {
  private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

  @Test
  void emailChannelAcceptsEmptyOptionalCredentialsSentByTheInterface() {
    var request = new CreateRequest("Основная почта", NotificationChannelType.EMAIL, "", "");

    assertTrue(validator.validate(request).isEmpty());
  }

  @Test
  void malformedNonEmptyUrlIsStillRejected() {
    var request = new CreateRequest("Webhook", NotificationChannelType.WEBHOOK,
        "http://example.com/hook", "secret");

    var violations = validator.validate(request);
    assertEquals(1, violations.size());
    assertEquals("url", violations.iterator().next().getPropertyPath().toString());
  }
}
