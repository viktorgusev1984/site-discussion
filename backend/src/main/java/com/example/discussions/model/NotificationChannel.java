package com.example.discussions.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
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
import java.time.Instant;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "notification_channels")
public class NotificationChannel {
  @Id @GeneratedValue(strategy = GenerationType.IDENTITY) public Long id;

  @JsonIgnore
  @ManyToOne(optional = false)
  @JoinColumn(name = "user_id", nullable = false)
  public User user;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  public NotificationChannelType type;

  @Column(nullable = false, length = 100)
  public String name;

  @Column(nullable = false)
  public boolean active = true;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(nullable = false)
  public JsonNode configuration = JsonNodeFactory.instance.objectNode();

  /** Encrypted at the service boundary; ciphertext is never serialized by Jackson. */
  @JsonIgnore
  @Column(name = "encrypted_secrets")
  public byte[] encryptedSecrets;

  @Column(name = "created_at", nullable = false)
  public Instant createdAt = Instant.now();

  @Column(name = "last_successful_check_at")
  public Instant lastSuccessfulCheckAt;
}
