package com.example.discussions.model;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "notification_deliveries", uniqueConstraints = @UniqueConstraint(name = "uk_notification_delivery_idempotency", columnNames = "idempotency_key"))
public class NotificationDelivery {
  public enum Status { PENDING, PROCESSING, DELIVERED, FAILED }

  @Id @GeneratedValue(strategy = GenerationType.IDENTITY) public Long id;
  @Column(name = "event_id", nullable = false, length = 100) public String eventId;
  @Column(name = "event_type", nullable = false, length = 40) public String eventType;
  @ManyToOne(optional = false) @JoinColumn(name = "recipient_user_id", nullable = false) public User recipient;
  @ManyToOne(optional = false) @JoinColumn(name = "channel_id", nullable = false) public NotificationChannel channel;
  @Enumerated(EnumType.STRING) @Column(name = "channel_type", nullable = false, length = 20) public NotificationChannelType channelType;
  @Column(nullable = false, columnDefinition = "text") public String payload;
  @Column(name = "idempotency_key", nullable = false, length = 200) public String idempotencyKey;
  @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) public Status status = Status.PENDING;
  @Column(nullable = false) public int attempts;
  @Column(name = "next_attempt_at", nullable = false) public Instant nextAttemptAt = Instant.now();
  @Column(name = "last_error", length = 1000) public String lastError;
  @Column(name = "created_at", nullable = false) public Instant createdAt = Instant.now();
  @Column(name = "delivered_at") public Instant deliveredAt;
}
