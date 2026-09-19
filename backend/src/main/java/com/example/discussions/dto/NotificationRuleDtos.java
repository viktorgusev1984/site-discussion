package com.example.discussions.dto;

import com.example.discussions.model.NotificationRule;
import com.example.discussions.model.NotificationScope;
import com.example.discussions.model.NotificationTrigger;
import jakarta.validation.constraints.NotNull;

public final class NotificationRuleDtos {
  private NotificationRuleDtos() {}

  public record CreateRequest(
      @NotNull NotificationTrigger trigger,
      @NotNull NotificationScope scope,
      Long discussionId,
      @NotNull Long channelId,
      Boolean active) {}

  public record UpdateRequest(
      @NotNull NotificationTrigger trigger,
      @NotNull NotificationScope scope,
      Long discussionId,
      @NotNull Long channelId,
      @NotNull Boolean active) {}

  public record View(Long id, NotificationTrigger trigger, NotificationScope scope,
      Long discussionId, Long channelId, boolean active) {
    public static View of(NotificationRule rule) {
      return new View(rule.id, rule.trigger, rule.scope,
          rule.discussion == null ? null : rule.discussion.id, rule.channel.id, rule.active);
    }
  }
}
