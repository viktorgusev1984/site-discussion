package com.example.discussions.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(
    name = "notification_rules",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_notification_rule",
        columnNames = {"user_id", "trigger", "scope", "discussion_id", "channel_id"}))
public class NotificationRule {
  @Id @GeneratedValue(strategy = GenerationType.IDENTITY) public Long id;

  @JsonIgnore
  @ManyToOne(optional = false)
  @JoinColumn(name = "user_id", nullable = false)
  public User user;

  @Enumerated(EnumType.STRING)
  @Column(name = "trigger", nullable = false, length = 40)
  public NotificationTrigger trigger;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  public NotificationScope scope;

  @JsonIgnore
  @ManyToOne
  @JoinColumn(name = "discussion_id")
  public Discussion discussion;

  @ManyToOne(optional = false)
  @JoinColumn(name = "channel_id", nullable = false)
  public NotificationChannel channel;

  @Column(nullable = false)
  public boolean active = true;
}
