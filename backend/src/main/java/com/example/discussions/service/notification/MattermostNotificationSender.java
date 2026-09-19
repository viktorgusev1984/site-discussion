package com.example.discussions.service.notification;

import com.example.discussions.model.*;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class MattermostNotificationSender implements NotificationSender {
  private final SafeWebhookClient client;
  private final ChannelSecrets secrets;
  public MattermostNotificationSender(SafeWebhookClient client, ChannelSecrets secrets) {
    this.client = client; this.secrets = secrets;
  }
  @Override public NotificationChannelType channelType() { return NotificationChannelType.MATTERMOST; }
  @Override public void send(NotificationDelivery delivery) throws Exception {
    String endpoint = secrets.decrypt(delivery.channel).path("url").asText();
    // Compatibility for channels created before endpoints were moved into encrypted_secrets.
    if (endpoint.isBlank()) endpoint = delivery.channel.configuration.path("url").asText();
    client.post(endpoint, delivery.payload, Map.of("Idempotency-Key", delivery.idempotencyKey));
  }
}
