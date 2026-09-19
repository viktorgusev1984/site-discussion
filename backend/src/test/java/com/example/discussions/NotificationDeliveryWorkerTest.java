package com.example.discussions;

import static org.mockito.Mockito.*;

import com.example.discussions.model.*;
import com.example.discussions.service.notification.*;
import java.time.Duration;
import java.util.*;
import org.junit.jupiter.api.Test;

class NotificationDeliveryWorkerTest {
  @Test void dispatchesByChannelType() throws Exception {
    NotificationService service = mock(NotificationService.class);
    NotificationSender email = mock(NotificationSender.class);
    when(email.channelType()).thenReturn(NotificationChannelType.EMAIL);
    NotificationDelivery delivery = new NotificationDelivery(); delivery.id = 42L; delivery.channelType = NotificationChannelType.EMAIL;
    when(service.claimNext()).thenReturn(Optional.of(delivery), Optional.empty());
    var properties = new NotificationProperties("", Duration.ofSeconds(1), 3, Duration.ofSeconds(2),
        Duration.ofSeconds(1), Duration.ofSeconds(1), 1024, 1);

    new NotificationDeliveryWorker(service, properties, List.of(email)).processDue();

    verify(email).send(delivery);
    verify(service).delivered(42L);
  }

  @Test void schedulesRetryWithoutLeakingExceptionMessage() throws Exception {
    NotificationService service = mock(NotificationService.class);
    NotificationSender webhook = mock(NotificationSender.class);
    when(webhook.channelType()).thenReturn(NotificationChannelType.WEBHOOK);
    NotificationDelivery delivery = new NotificationDelivery(); delivery.id = 7L; delivery.channelType = NotificationChannelType.WEBHOOK;
    when(service.claimNext()).thenReturn(Optional.of(delivery), Optional.empty());
    doThrow(new IllegalStateException("https://secret.example/token/full-payload")).when(webhook).send(delivery);
    var properties = new NotificationProperties("", Duration.ofSeconds(1), 3, Duration.ofSeconds(2),
        Duration.ofSeconds(1), Duration.ofSeconds(1), 1024, 1);

    new NotificationDeliveryWorker(service, properties, List.of(webhook)).processDue();

    verify(service).failed(7L, "Delivery failed (IllegalStateException)", 3, Duration.ofSeconds(2));
  }
}
