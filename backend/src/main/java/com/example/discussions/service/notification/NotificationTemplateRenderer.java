package com.example.discussions.service.notification;

import com.example.discussions.model.NotificationChannelType;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Component;

/** Renders the neutral event without ever serializing a User or another persistence entity. */
@Component
public class NotificationTemplateRenderer {
  private final ObjectMapper json;

  public NotificationTemplateRenderer(ObjectMapper json) { this.json = json; }

  public String render(NotificationEvent event, NotificationChannelType channel) {
    return switch (channel) {
      case WEBHOOK -> write(canonicalPayload(event));
      case MATTERMOST -> write(Map.of("text", message(event), "event", canonicalPayload(event)));
      case EMAIL -> message(event);
    };
  }

  private Map<String, Object> canonicalPayload(NotificationEvent event) {
    Map<String, Object> payload = new LinkedHashMap<>();
    payload.put("schemaVersion", 1);
    payload.put("eventId", event.id());
    payload.put("type", event.trigger().name());
    payload.put("actorId", event.actorId());
    payload.put("recipientId", event.recipientId());
    payload.put("discussionId", event.discussionId());
    if (event.commentId() != null) payload.put("commentId", event.commentId());
    payload.put("occurredAt", event.occurredAt().toString());
    payload.put("data", event.data());
    return payload;
  }

  private String message(NotificationEvent event) {
    var data = event.data();
    return switch (event.trigger()) {
      case NEW_DISCUSSION -> "Новое обсуждение: " + data.discussionTitle();
      case NEW_COMMENT -> data.actorDisplayName() + " оставил комментарий в «" + data.discussionTitle() + "»";
      case NEW_REPLY -> data.actorDisplayName() + " ответил на ваш комментарий в «" + data.discussionTitle() + "»";
      case FIRST_VOTE -> data.actorDisplayName() + " проголосовал в «" + data.discussionTitle() + "»";
      case NEW_REACTION -> data.actorDisplayName() + " добавил реакцию " + data.emoji() + " в «" + data.discussionTitle() + "»";
      case STATUS_CHANGED -> "Статус «" + data.discussionTitle() + "» изменён на " + data.status();
      case JIRA_ACTION_CREATED -> "Для «" + data.discussionTitle() + "» создано Jira-действие " + data.actionLabel();
      case MENTION -> "Вас упомянули в «" + data.discussionTitle() + "»";
    };
  }

  private String write(Object value) {
    try { return json.writeValueAsString(value); }
    catch (JsonProcessingException e) { throw new IllegalArgumentException("Notification payload cannot be serialized", e); }
  }
}
