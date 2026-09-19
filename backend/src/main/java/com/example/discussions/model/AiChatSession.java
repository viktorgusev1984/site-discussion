package com.example.discussions.model;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "ai_chat_sessions")
public class AiChatSession {
  @Id @GeneratedValue(strategy = GenerationType.IDENTITY) public Long id;
  @Column(name = "session_id", nullable = false, unique = true, length = 64) public String sessionId;
  @ManyToOne(optional = false) @JoinColumn(name = "user_id") public User user;
  @Column(name = "created_at", nullable = false) public Instant createdAt = Instant.now();
}
