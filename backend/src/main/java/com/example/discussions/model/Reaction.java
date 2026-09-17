package com.example.discussions.model;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "reactions")
public class Reaction {
  @Id @GeneratedValue(strategy = GenerationType.IDENTITY) public Long id;
  @ManyToOne(optional = false) @JoinColumn(name = "user_id") public User user;
  @ManyToOne @JoinColumn(name = "discussion_id") public Discussion discussion;
  @ManyToOne @JoinColumn(name = "comment_id") public Comment comment;
  @Column(nullable = false, length = 16) public String emoji;
  @Column(name = "created_at", nullable = false) public Instant createdAt = Instant.now();
}
