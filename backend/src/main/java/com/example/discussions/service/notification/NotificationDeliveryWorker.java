package com.example.discussions.service.notification;

import com.example.discussions.model.*;
import java.util.*;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class NotificationDeliveryWorker {
  private final NotificationService service;
  private final NotificationProperties properties;
  private final Map<NotificationChannelType, NotificationSender> senders;

  public NotificationDeliveryWorker(NotificationService service, NotificationProperties properties, List<NotificationSender> senders) {
    this.service = service; this.properties = properties;
    this.senders = new EnumMap<>(NotificationChannelType.class);
    senders.forEach(sender -> this.senders.put(sender.channelType(), sender));
  }

  @Scheduled(fixedDelayString = "${app.notifications.worker-interval:10s}")
  public void processDue() {
    for (int count = 0; count < 100; count++) {
      Optional<NotificationDelivery> next = service.claimNext();
      if (next.isEmpty()) return;
      NotificationDelivery delivery = next.get();
      try {
        NotificationSender sender = senders.get(delivery.channelType);
        if (sender == null) throw new IllegalStateException("Unsupported notification channel");
        sender.send(delivery);
        service.delivered(delivery.id);
      } catch (Exception e) {
        // Persist only an exception class, never a URL, token, Authorization header, or payload.
        service.failed(delivery.id, "Delivery failed (" + e.getClass().getSimpleName() + ")", properties.attempts(), properties.initialBackoff());
      }
    }
  }
}
