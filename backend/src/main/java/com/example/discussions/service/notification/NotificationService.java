package com.example.discussions.service.notification;

import com.example.discussions.model.*;
import com.example.discussions.repository.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;

@Service
public class NotificationService {
  private final NotificationRuleRepository rules;
  private final NotificationDeliveryRepository deliveries;
  private final ObjectMapper json;
  private final SafeWebhookClient webhookClient;

  public NotificationService(NotificationRuleRepository rules, NotificationDeliveryRepository deliveries,
      ObjectMapper json, SafeWebhookClient webhookClient) {
    this.rules = rules; this.deliveries = deliveries; this.json = json; this.webhookClient = webhookClient;
  }

  /** Must be called inside the transaction that mutates the discussion. */
  @Transactional(propagation = Propagation.MANDATORY)
  public void enqueue(NotificationTrigger trigger, Discussion discussion, Object payload, String eventId) {
    String serialized;
    try { serialized = json.writeValueAsString(payload); }
    catch (JsonProcessingException e) { throw new IllegalArgumentException("Notification payload cannot be serialized", e); }
    for (NotificationRule rule : rules.findMatching(trigger, discussion.id)) {
      NotificationDelivery delivery = new NotificationDelivery();
      delivery.eventId = eventId;
      delivery.eventType = trigger.name();
      delivery.recipient = rule.user;
      delivery.channel = rule.channel;
      delivery.channelType = rule.channel.type;
      delivery.payload = serialized;
      delivery.idempotencyKey = eventId + ":" + rule.user.id + ":" + rule.channel.id;
      if (!deliveries.existsByIdempotencyKey(delivery.idempotencyKey)) deliveries.save(delivery);
    }
  }

  public void validateChannelConfiguration(NotificationChannel channel) {
    if (channel.type == NotificationChannelType.WEBHOOK || channel.type == NotificationChannelType.MATTERMOST) {
      try { webhookClient.validate(channel.configuration.path("url").asText()); }
      catch (Exception e) { throw new IllegalArgumentException("Invalid or unsafe notification endpoint", e); }
    }
  }

  @Transactional
  public Optional<NotificationDelivery> claimNext() {
    Optional<NotificationDelivery> claimed = deliveries.findNextDueForUpdate(Instant.now());
    claimed.ifPresent(d -> { d.status = NotificationDelivery.Status.PROCESSING; d.attempts++; });
    return claimed;
  }

  @Transactional
  public void delivered(Long id) {
    NotificationDelivery d = deliveries.findById(id).orElseThrow();
    d.status = NotificationDelivery.Status.DELIVERED; d.deliveredAt = Instant.now(); d.lastError = null;
  }

  @Transactional
  public void failed(Long id, String safeError, int maxAttempts, java.time.Duration initialBackoff) {
    NotificationDelivery d = deliveries.findById(id).orElseThrow();
    d.lastError = safeError.substring(0, Math.min(safeError.length(), 1000));
    if (d.attempts >= maxAttempts) d.status = NotificationDelivery.Status.FAILED;
    else {
      d.status = NotificationDelivery.Status.PENDING;
      long multiplier = 1L << Math.min(d.attempts - 1, 20);
      d.nextAttemptAt = Instant.now().plus(initialBackoff.multipliedBy(multiplier));
    }
  }
}
