package com.example.discussions;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.example.discussions.controller.NotificationSettingsController;
import com.example.discussions.exception.ApiException;
import com.example.discussions.model.*;
import com.example.discussions.repository.*;
import com.example.discussions.service.CurrentUser;
import com.example.discussions.service.notification.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.util.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.context.SecurityContextHolder;

class NotificationSettingsAuthorizationTest {
  @AfterEach void clearSecurity() { SecurityContextHolder.clearContext(); }

  @Test void anonymousControllerRequestIsRejected() {
    var controller = new NotificationSettingsController(new CurrentUser(mock(UserRepository.class)),
        mock(NotificationSettingsService.class));
    ApiException error = assertThrows(ApiException.class, controller::channels);
    assertEquals(401, error.status.value());
  }

  @Test void readsAndMutationsAreAlwaysScopedToTheOwner() {
    var channels=mock(NotificationChannelRepository.class); var rules=mock(NotificationRuleRepository.class);
    var service=service(channels,rules); var owner=user(10L);
    when(channels.findByUserIdOrderByCreatedAtAsc(10L)).thenReturn(List.of());
    when(rules.findByUserIdOrderByIdAsc(10L)).thenReturn(List.of());
    service.channels(owner); service.rules(owner);
    verify(channels).findByUserIdOrderByCreatedAtAsc(10L);
    verify(rules).findByUserIdOrderByIdAsc(10L);

    when(channels.findByIdAndUserId(99L,10L)).thenReturn(Optional.empty());
    assertEquals(404, assertThrows(ApiException.class,()->service.disable(owner,99L)).status.value());
    when(rules.findByIdAndUserId(88L,10L)).thenReturn(Optional.empty());
    assertEquals(404, assertThrows(ApiException.class,()->service.deleteRule(owner,88L)).status.value());
    verify(channels,never()).findById(99L); verify(rules,never()).findById(88L);
  }

  private NotificationSettingsService service(NotificationChannelRepository channels,NotificationRuleRepository rules) {
    var props=new NotificationProperties("key",Duration.ofSeconds(1),3,Duration.ofSeconds(1),Duration.ofSeconds(1),Duration.ofSeconds(1),1024,1,Duration.ofMinutes(1));
    var json=new ObjectMapper();
    return new NotificationSettingsService(channels,rules,mock(DiscussionRepository.class),new ChannelSecrets(json,props),
        mock(SafeWebhookClient.class),props,json,List.of());
  }
  private User user(long id) { var user=new User(); user.id=id; return user; }
}
