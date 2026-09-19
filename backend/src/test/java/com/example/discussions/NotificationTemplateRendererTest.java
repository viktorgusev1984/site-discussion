package com.example.discussions;

import static org.junit.jupiter.api.Assertions.*;

import com.example.discussions.model.*;
import com.example.discussions.service.notification.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class NotificationTemplateRendererTest {
  private final NotificationTemplateRenderer renderer = new NotificationTemplateRenderer(new ObjectMapper());
  private final NotificationEvent event = new NotificationEvent("reaction:7", NotificationTrigger.NEW_REACTION,
      10L, 20L, 30L, 40L, Instant.parse("2026-09-19T08:00:00Z"),
      new NotificationEvent.TemplateData("Safe title", "Actor", null, "👍", null));

  @Test void webhookGetsCanonicalSafePayload() throws Exception {
    var payload = new ObjectMapper().readTree(renderer.render(event, NotificationChannelType.WEBHOOK));
    assertEquals(1, payload.path("schemaVersion").asInt());
    assertEquals(10L, payload.path("actorId").asLong());
    assertEquals(20L, payload.path("recipientId").asLong());
    assertEquals("👍", payload.path("data").path("emoji").asText());
    assertFalse(payload.toString().contains("email"));
    assertFalse(payload.toString().contains("password"));
    assertFalse(payload.toString().contains("jwt"));
  }

  @Test void channelsHaveIndependentRepresentations() {
    String webhook = renderer.render(event, NotificationChannelType.WEBHOOK);
    String email = renderer.render(event, NotificationChannelType.EMAIL);
    String mattermost = renderer.render(event, NotificationChannelType.MATTERMOST);
    assertNotEquals(webhook, email);
    assertNotEquals(webhook, mattermost);
    assertTrue(email.contains("Actor"));
    assertTrue(mattermost.contains("\"text\""));
  }
}
