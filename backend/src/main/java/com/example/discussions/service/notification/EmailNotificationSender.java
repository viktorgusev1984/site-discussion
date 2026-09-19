package com.example.discussions.service.notification;

import com.example.discussions.model.*;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

@Component
public class EmailNotificationSender implements NotificationSender {
  private final JavaMailSender mail;
  public EmailNotificationSender(JavaMailSender mail) { this.mail = mail; }
  @Override public NotificationChannelType channelType() { return NotificationChannelType.EMAIL; }
  @Override public void send(NotificationDelivery delivery) {
    SimpleMailMessage message = new SimpleMailMessage();
    message.setTo(delivery.recipient.email);
    message.setSubject("Discussion notification: " + delivery.eventType);
    message.setText(delivery.payload);
    mail.send(message);
  }
}
