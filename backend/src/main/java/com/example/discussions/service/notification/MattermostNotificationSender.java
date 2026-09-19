package com.example.discussions.service.notification;

import com.example.discussions.model.*;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class MattermostNotificationSender implements NotificationSender {
  private final SafeWebhookClient client;
  public MattermostNotificationSender(SafeWebhookClient client) { this.client = client; }
  @Override public NotificationChannelType channelType() { return NotificationChannelType.MATTERMOST; }
  @Override public void send(NotificationDelivery delivery) throws Exception {
    String endpoint = delivery.channel.configuration.path("url").asText();
    client.post(endpoint, delivery.payload, Map.of("Idempotency-Key", delivery.idempotencyKey));
  }
}
