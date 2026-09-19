package com.example.discussions;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.example.discussions.model.*;
import com.example.discussions.repository.*;
import com.example.discussions.service.notification.*;
import java.time.Instant;
import java.util.List;
import org.mockito.ArgumentCaptor;
import org.junit.jupiter.api.Test;

class NotificationServiceTest {
  private final NotificationRuleRepository rules = mock(NotificationRuleRepository.class);
  private final NotificationDeliveryRepository deliveries = mock(NotificationDeliveryRepository.class);
  private final DiscussionAccessService access = mock(DiscussionAccessService.class);
  private final NotificationTemplateRenderer renderer = mock(NotificationTemplateRenderer.class);
  private final NotificationService service = new NotificationService(rules, deliveries,
      mock(SafeWebhookClient.class), access, renderer);

  @Test void suppressesSelfNotificationsBeforeLookingUpRules() {
    var discussion = discussion(5L);
    service.publish(event(1L, 1L), discussion);
    verifyNoInteractions(rules, deliveries, access, renderer);
  }

  @Test void checksAccessImmediatelyBeforeCreatingDelivery() {
    var discussion = discussion(5L);
    var recipient = user(2L);
    var channel = new NotificationChannel(); channel.id = 9L; channel.type = NotificationChannelType.WEBHOOK;
    var rule = new NotificationRule(); rule.user = recipient; rule.channel = channel;
    when(rules.findActiveForDiscussion(2L, NotificationTrigger.NEW_COMMENT, 5L)).thenReturn(List.of(rule));
    when(access.canView(recipient, discussion)).thenReturn(false);

    service.publish(event(1L, 2L), discussion);

    verify(access).canView(recipient, discussion);
    verifyNoInteractions(renderer, deliveries);
  }

  @Test void duplicatePublicationDoesNotCreateAnotherOutboxRow() {
    var discussion=discussion(5L); var recipient=user(2L);
    var channel=new NotificationChannel(); channel.id=9L; channel.type=NotificationChannelType.WEBHOOK;
    var rule=new NotificationRule(); rule.user=recipient; rule.channel=channel;
    when(rules.findActiveForDiscussion(2L,NotificationTrigger.NEW_COMMENT,5L)).thenReturn(List.of(rule));
    when(access.canView(recipient,discussion)).thenReturn(true);
    when(deliveries.existsByIdempotencyKey("comment:1:2:9")).thenReturn(true);
    service.publish(event(1L,2L),discussion);
    verify(deliveries,never()).save(any());
  }

  @Test void createsAnOutboxRowWithAStableIdempotencyKey() {
    var discussion=discussion(5L); var recipient=user(2L);
    var channel=new NotificationChannel(); channel.id=9L; channel.type=NotificationChannelType.WEBHOOK;
    var rule=new NotificationRule(); rule.user=recipient; rule.channel=channel;
    when(rules.findActiveForDiscussion(2L,NotificationTrigger.NEW_COMMENT,5L)).thenReturn(List.of(rule));
    when(access.canView(recipient,discussion)).thenReturn(true); when(renderer.render(any(),eq(channel.type))).thenReturn("{}");
    service.publish(event(1L,2L),discussion);
    var captor=ArgumentCaptor.forClass(NotificationDelivery.class); verify(deliveries).save(captor.capture());
    assertEquals("comment:1:2:9",captor.getValue().idempotencyKey);
  }

  private NotificationEvent event(Long actor, Long recipient) {
    return new NotificationEvent("comment:1", NotificationTrigger.NEW_COMMENT, actor, recipient, 5L, 3L,
        Instant.EPOCH, new NotificationEvent.TemplateData("Title", "Actor", null, null, null));
  }
  private Discussion discussion(Long id) { var value = new Discussion(); value.id = id; return value; }
  private User user(Long id) { var value = new User(); value.id = id; return value; }
}
