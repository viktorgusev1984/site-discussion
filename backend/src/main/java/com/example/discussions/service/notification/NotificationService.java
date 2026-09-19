package com.example.discussions.service.notification;

import com.example.discussions.model.*;
import com.example.discussions.repository.*;
import java.time.Instant;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;

@Service
public class NotificationService {
  private final NotificationRuleRepository rules;
  private final NotificationDeliveryRepository deliveries;
  private final SafeWebhookClient webhookClient;
  private final DiscussionAccessService access;
  private final NotificationTemplateRenderer renderer;

  public NotificationService(NotificationRuleRepository rules, NotificationDeliveryRepository deliveries,
      SafeWebhookClient webhookClient, DiscussionAccessService access, NotificationTemplateRenderer renderer) {
    this.rules = rules; this.deliveries = deliveries; this.webhookClient = webhookClient;
    this.access = access; this.renderer = renderer;
  }

  /** Must be called inside the transaction that mutates the discussion. */
  @Transactional(propagation = Propagation.MANDATORY)
  public void publish(NotificationEvent event, Discussion discussion) {
    if (!event.discussionId().equals(discussion.id)) throw new IllegalArgumentException("Event discussion mismatch");
    if (event.actorId().equals(event.recipientId())) return;
    for (NotificationRule rule : rules.findActiveForDiscussion(event.recipientId(), event.trigger(), discussion.id)) {
      // Deliberately check at the last possible point, after rule selection and before outbox creation.
      if (!access.canView(rule.user, discussion)) continue;
      NotificationDelivery delivery = new NotificationDelivery();
      delivery.eventId = event.id();
      delivery.eventType = event.trigger().name();
      delivery.recipient = rule.user;
      delivery.channel = rule.channel;
      delivery.channelType = rule.channel.type;
      delivery.payload = renderer.render(event, rule.channel.type);
      delivery.idempotencyKey = event.id() + ":" + rule.user.id + ":" + rule.channel.id;
      if (!deliveries.existsByIdempotencyKey(delivery.idempotencyKey)) deliveries.save(delivery);
    }
  }

  /** Fan-out is retained for the global NEW_DISCUSSION subscription. */
  @Transactional(propagation = Propagation.MANDATORY)
  public void publishNewDiscussion(User actor, Discussion discussion, Instant occurredAt) {
    rules.findMatching(NotificationTrigger.NEW_DISCUSSION, discussion.id).stream()
        .map(rule -> rule.user).filter(user -> !user.id.equals(actor.id)).distinct()
        .forEach(recipient -> publish(new NotificationEvent(
            "discussion-created:" + discussion.id, NotificationTrigger.NEW_DISCUSSION,
            actor.id, recipient.id, discussion.id, null, occurredAt,
            new NotificationEvent.TemplateData(discussion.title, actor.displayName, null, null, null)), discussion));
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
