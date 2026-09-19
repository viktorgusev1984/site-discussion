package com.example.discussions.service.notification;

import com.example.discussions.dto.NotificationChannelDtos;
import com.example.discussions.dto.NotificationRuleDtos;
import com.example.discussions.exception.ApiException;
import com.example.discussions.model.*;
import com.example.discussions.repository.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.net.URI;
import java.time.Instant;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NotificationSettingsService {
  private final NotificationChannelRepository channels;
  private final NotificationRuleRepository rules;
  private final DiscussionRepository discussions;
  private final ChannelSecrets secrets;
  private final SafeWebhookClient webhookClient;
  private final NotificationProperties properties;
  private final ObjectMapper json;
  private final Map<NotificationChannelType, NotificationSender> senders;

  public NotificationSettingsService(NotificationChannelRepository channels,
      NotificationRuleRepository rules, DiscussionRepository discussions, ChannelSecrets secrets,
      SafeWebhookClient webhookClient, NotificationProperties properties, ObjectMapper json,
      List<NotificationSender> senders) {
    this.channels = channels;
    this.rules = rules;
    this.discussions = discussions;
    this.secrets = secrets;
    this.webhookClient = webhookClient;
    this.properties = properties;
    this.json = json;
    this.senders = new EnumMap<>(NotificationChannelType.class);
    senders.forEach(sender -> this.senders.put(sender.channelType(), sender));
  }

  @Transactional(readOnly = true)
  public List<NotificationChannelDtos.View> channels(User owner) {
    return channels.findByUserIdOrderByCreatedAtAsc(owner.id).stream()
        .map(NotificationChannelDtos.View::of).toList();
  }

  @Transactional
  public NotificationChannelDtos.View create(User owner, NotificationChannelDtos.CreateRequest input) {
    NotificationChannel channel = new NotificationChannel();
    channel.user = owner;
    apply(channel, input.name(), input.type(), input.url(), input.secret(), false);
    return NotificationChannelDtos.View.of(channels.save(channel));
  }

  @Transactional
  public NotificationChannelDtos.View update(User owner, Long id,
      NotificationChannelDtos.UpdateRequest input) {
    NotificationChannel channel = channel(owner, id);
    apply(channel, input.name(), input.type(), input.url(), input.secret(), true);
    channel.active = input.active();
    return NotificationChannelDtos.View.of(channel);
  }

  @Transactional
  public NotificationChannelDtos.View disable(User owner, Long id) {
    NotificationChannel channel = channel(owner, id);
    channel.active = false;
    return NotificationChannelDtos.View.of(channel);
  }

  @Transactional
  public void deleteChannel(User owner, Long id) {
    channels.delete(channel(owner, id));
    channels.flush();
  }

  @Transactional(readOnly = true)
  public List<NotificationRuleDtos.View> rules(User owner) {
    return rules.findByUserIdOrderByIdAsc(owner.id).stream().map(NotificationRuleDtos.View::of).toList();
  }

  @Transactional
  public NotificationRuleDtos.View createRule(User owner, NotificationRuleDtos.CreateRequest input) {
    NotificationRule rule = new NotificationRule();
    rule.user = owner;
    apply(rule, owner, input.trigger(), input.scope(), input.discussionId(), input.channelId());
    rule.active = input.active() == null || input.active();
    return NotificationRuleDtos.View.of(rules.save(rule));
  }

  @Transactional
  public NotificationRuleDtos.View updateRule(User owner, Long id,
      NotificationRuleDtos.UpdateRequest input) {
    NotificationRule rule = rule(owner, id);
    apply(rule, owner, input.trigger(), input.scope(), input.discussionId(), input.channelId());
    rule.active = input.active();
    return NotificationRuleDtos.View.of(rule);
  }

  @Transactional
  public void deleteRule(User owner, Long id) {
    rules.delete(rule(owner, id));
  }

  /** Claims the rate-limit slot in the database before using the production sender adapter. */
  @Transactional(noRollbackFor = ApiException.class)
  public void test(User owner, Long id) {
    Instant now = Instant.now();
    if (channels.claimCheck(id, owner.id, now, now.minus(properties.testInterval())) == 0) {
      if (channels.findByIdAndUserId(id, owner.id).isEmpty()) throw notFound("Канал не найден");
      throw new ApiException(HttpStatus.TOO_MANY_REQUESTS,
          "Проверять канал можно не чаще одного раза за " + properties.testInterval());
    }
    NotificationChannel channel = channel(owner, id);
    NotificationSender sender = senders.get(channel.type);
    if (sender == null) throw ApiException.badRequest("Тип канала не поддерживается");
    NotificationDelivery probe = new NotificationDelivery();
    probe.id = 0L;
    probe.eventId = "channel-test-" + UUID.randomUUID();
    probe.eventType = "CHANNEL_TEST";
    probe.recipient = owner;
    probe.channel = channel;
    probe.channelType = channel.type;
    probe.payload = channel.type == NotificationChannelType.MATTERMOST
        ? "{\"text\":\"Проверка канала уведомлений\"}"
        : channel.type == NotificationChannelType.WEBHOOK
            ? "{\"schemaVersion\":1,\"type\":\"CHANNEL_TEST\"}"
            : "Проверка канала уведомлений";
    probe.idempotencyKey = probe.eventId;
    try {
      sender.send(probe);
      channel.lastSuccessfulCheckAt = now;
    } catch (Exception exception) {
      // Do not reflect exception messages: HTTP clients and mail transports may include credentials.
      throw new ApiException(HttpStatus.BAD_GATEWAY, "Не удалось проверить канал уведомлений");
    }
  }

  private void apply(NotificationChannel channel, String name, NotificationChannelType type,
      String url, String secret, boolean retainBlank) {
    String cleanName = name.trim();
    ObjectNode credentials = json.createObjectNode();
    if (retainBlank && channel.encryptedSecrets != null) {
      try { credentials.setAll((ObjectNode) secrets.decrypt(channel)); }
      catch (Exception e) { throw new IllegalStateException("Stored channel credentials are invalid", e); }
    }
    if (url != null && !url.isBlank()) credentials.put("url", url.trim());
    if (secret != null && !secret.isBlank()) credentials.put("hmacSecret", secret);

    if (type != NotificationChannelType.EMAIL) {
      String endpoint = credentials.path("url").asText();
      if (endpoint.isBlank()) throw ApiException.badRequest("Для webhook-канала требуется URL");
      try { webhookClient.validate(endpoint); }
      catch (Exception e) { throw ApiException.badRequest("URL канала недоступен или небезопасен"); }
      if (type == NotificationChannelType.WEBHOOK
          && credentials.path("hmacSecret").asText().isBlank()) {
        throw ApiException.badRequest("Для webhook-канала требуется секрет подписи");
      }
      channel.connectionDescription = maskedEndpoint(endpoint);
    } else {
      credentials.removeAll();
      channel.connectionDescription = maskedEmail(channel.user.email);
    }
    try { channel.encryptedSecrets = secrets.encrypt(credentials); }
    catch (Exception e) { throw new IllegalStateException("Channel credentials cannot be encrypted", e); }
    channel.configuration = json.createObjectNode();
    channel.name = cleanName;
    channel.type = type;
  }

  private void apply(NotificationRule rule, User owner, NotificationTrigger trigger,
      NotificationScope scope, Long discussionId, Long channelId) {
    rule.trigger = trigger;
    rule.scope = scope;
    rule.channel = channel(owner, channelId);
    if (scope == NotificationScope.ALL_DISCUSSIONS) {
      if (discussionId != null) throw ApiException.badRequest("Для общего правила discussionId не задаётся");
      rule.discussion = null;
    } else {
      if (discussionId == null) throw ApiException.badRequest("Для правила обсуждения нужен discussionId");
      rule.discussion = discussions.findById(discussionId)
          .orElseThrow(() -> notFound("Обсуждение не найдено"));
    }
  }

  private NotificationChannel channel(User owner, Long id) {
    return channels.findByIdAndUserId(id, owner.id).orElseThrow(() -> notFound("Канал не найден"));
  }

  private NotificationRule rule(User owner, Long id) {
    return rules.findByIdAndUserId(id, owner.id).orElseThrow(() -> notFound("Правило не найдено"));
  }

  private ApiException notFound(String message) { return ApiException.notFound(message); }

  private String maskedEndpoint(String endpoint) {
    URI uri = URI.create(endpoint);
    String path = Optional.ofNullable(uri.getPath()).orElse("").replaceAll("/+$", "");
    String identifier = path.substring(path.lastIndexOf('/') + 1);
    String tail = identifier.length() <= 4 ? identifier : identifier.substring(identifier.length() - 4);
    return uri.getHost() + (tail.isBlank() ? "" : "/…" + tail);
  }

  private String maskedEmail(String email) {
    int at = email.lastIndexOf('@');
    String local = at > 0 ? email.substring(0, at) : email;
    String domain = at > 0 ? email.substring(at + 1) : "";
    String tail = local.substring(Math.max(0, local.length() - 2));
    return "…" + tail + (domain.isBlank() ? "" : "@" + domain);
  }
}
