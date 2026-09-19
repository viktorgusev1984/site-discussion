package com.example.discussions.service.notification;

import com.example.discussions.model.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

@Component
public class EmailNotificationSender implements NotificationSender {
  private static final String FROM_NAME = "open-ideas";
  private final JavaMailSender mail;
  private final String smtpUsername;
  public EmailNotificationSender(JavaMailSender mail,
      @Value("${spring.mail.username:}") String smtpUsername) {
    this.mail = mail;
    this.smtpUsername = smtpUsername;
  }
  @Override public NotificationChannelType channelType() { return NotificationChannelType.EMAIL; }
  @Override public void send(NotificationDelivery delivery) {
    SimpleMailMessage message = new SimpleMailMessage();
    message.setFrom(smtpUsername.isBlank() ? FROM_NAME : FROM_NAME + " <" + smtpUsername + ">");
    message.setTo(delivery.recipient.email);
    message.setSubject("Discussion notification: " + delivery.eventType);
    message.setText(delivery.payload);
    mail.send(message);
  }
}
