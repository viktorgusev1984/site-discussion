package com.example.discussions.service.notification;

import com.example.discussions.model.NotificationTrigger;
import java.time.Instant;
import java.util.Objects;

/** A channel-neutral event. Only explicitly safe, presentation-oriented data belongs here. */
public record NotificationEvent(
    String id,
    NotificationTrigger trigger,
    Long actorId,
    Long recipientId,
    Long discussionId,
    Long commentId,
    Instant occurredAt,
    TemplateData data) {

  public NotificationEvent {
    Objects.requireNonNull(id);
    Objects.requireNonNull(trigger);
    Objects.requireNonNull(actorId);
    Objects.requireNonNull(recipientId);
    Objects.requireNonNull(discussionId);
    Objects.requireNonNull(occurredAt);
    Objects.requireNonNull(data);
  }

  /** Deliberately excludes credentials, e-mail addresses and arbitrary entity serialization. */
  public record TemplateData(
      String discussionTitle,
      String actorDisplayName,
      String status,
      String emoji,
      String actionLabel) {}
}
