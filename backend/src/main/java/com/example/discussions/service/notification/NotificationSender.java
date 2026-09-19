package com.example.discussions.service.notification;

import com.example.discussions.model.NotificationChannelType;
import com.example.discussions.model.NotificationDelivery;

public interface NotificationSender {
  NotificationChannelType channelType();
  void send(NotificationDelivery delivery) throws Exception;
}
