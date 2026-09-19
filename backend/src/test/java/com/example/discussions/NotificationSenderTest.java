package com.example.discussions;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.example.discussions.model.*;
import com.example.discussions.service.notification.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

class NotificationSenderTest {
  @Test void emailNotificationsAlwaysUseApplicationSender() {
    var mail = mock(JavaMailSender.class);
    var recipient = new User(); recipient.email = "recipient@example.com";
    var delivery = new NotificationDelivery();
    delivery.recipient = recipient; delivery.eventType = "CHANNEL_TEST"; delivery.payload = "Test";

    new EmailNotificationSender(mail, "sender@example.com").send(delivery);

    var message = ArgumentCaptor.forClass(SimpleMailMessage.class);
    verify(mail).send(message.capture());
    assertEquals("open-ideas <sender@example.com>", message.getValue().getFrom());
    assertArrayEquals(new String[] {"recipient@example.com"}, message.getValue().getTo());
  }

  @Test void customWebhookHasPayloadIdempotencyAndValidHmac() throws Exception {
    var client = mock(SafeWebhookClient.class);
    var secrets = secrets();
    var channel = channel(NotificationChannelType.WEBHOOK);
    channel.encryptedSecrets = secrets.encrypt(new ObjectMapper().readTree(
        "{\"url\":\"https://hooks.example/notify\",\"hmacSecret\":\"shared-key\"}"));
    var delivery = delivery(channel, "{\"schemaVersion\":1}");

    new WebhookNotificationSender(client, secrets).send(delivery);

    @SuppressWarnings("unchecked") ArgumentCaptor<Map<String,String>> headers = ArgumentCaptor.forClass(Map.class);
    verify(client).post(eq("https://hooks.example/notify"), eq(delivery.payload), headers.capture());
    assertEquals(delivery.idempotencyKey, headers.getValue().get("Idempotency-Key"));
    assertEquals(delivery.eventId, headers.getValue().get("X-Notification-Event-Id"));
    String timestamp = headers.getValue().get("X-Notification-Timestamp");
    Mac mac = Mac.getInstance("HmacSHA256");
    mac.init(new SecretKeySpec("shared-key".getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
    assertEquals("v1=" + java.util.HexFormat.of().formatHex(mac.doFinal(
        (timestamp + "." + delivery.payload).getBytes(StandardCharsets.UTF_8))),
        headers.getValue().get("X-Notification-Signature"));
  }

  @Test void mattermostForwardsRenderedPayloadAndIdempotencyKey() throws Exception {
    var client = mock(SafeWebhookClient.class); var secrets = secrets();
    var channel = channel(NotificationChannelType.MATTERMOST);
    channel.encryptedSecrets = secrets.encrypt(new ObjectMapper().readTree("{\"url\":\"https://mattermost.example/hook\"}"));
    var delivery = delivery(channel, "{\"text\":\"A new reply\"}");
    new MattermostNotificationSender(client, secrets).send(delivery);
    verify(client).post("https://mattermost.example/hook", delivery.payload,
        Map.of("Idempotency-Key", delivery.idempotencyKey));
  }

  private ChannelSecrets secrets() { return new ChannelSecrets(new ObjectMapper(), new NotificationProperties(
      "test-encryption-key", Duration.ofSeconds(1), 3, Duration.ofSeconds(1), Duration.ofSeconds(1),
      Duration.ofSeconds(1), 1024, 1, null)); }
  private NotificationChannel channel(NotificationChannelType type) { var c=new NotificationChannel(); c.type=type; return c; }
  private NotificationDelivery delivery(NotificationChannel c,String payload) { var d=new NotificationDelivery();
    d.channel=c; d.channelType=c.type; d.payload=payload; d.eventId="comment:12"; d.idempotencyKey="comment:12:2:7"; return d; }
}
