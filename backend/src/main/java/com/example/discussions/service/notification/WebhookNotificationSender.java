package com.example.discussions.service.notification;

import com.example.discussions.model.*;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.stereotype.Component;

@Component
public class WebhookNotificationSender implements NotificationSender {
  private final SafeWebhookClient client;
  private final ChannelSecrets secrets;
  public WebhookNotificationSender(SafeWebhookClient client, ChannelSecrets secrets) { this.client = client; this.secrets = secrets; }
  @Override public NotificationChannelType channelType() { return NotificationChannelType.WEBHOOK; }
  @Override public void send(NotificationDelivery delivery) throws Exception {
    var credentials = secrets.decrypt(delivery.channel);
    String endpoint = credentials.path("url").asText();
    // Compatibility for channels created before endpoints were moved into encrypted_secrets.
    if (endpoint.isBlank()) endpoint = delivery.channel.configuration.path("url").asText();
    String timestamp = Long.toString(Instant.now().getEpochSecond());
    String secret = credentials.path("hmacSecret").asText();
    if (secret.isBlank()) throw new IllegalStateException("Webhook signing secret is missing");
    Mac mac = Mac.getInstance("HmacSHA256");
    mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
    String signature = HexFormat.of().formatHex(mac.doFinal((timestamp + "." + delivery.payload).getBytes(StandardCharsets.UTF_8)));
    client.post(endpoint, delivery.payload, Map.of(
        "X-Notification-Event-Id", delivery.eventId,
        "X-Notification-Timestamp", timestamp,
        "X-Notification-Schema-Version", "1",
        "X-Notification-Signature", "v1=" + signature,
        "Idempotency-Key", delivery.idempotencyKey));
  }
}
