package com.example.discussions.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "discussion_actions")
public class DiscussionAction {
  @Id @GeneratedValue(strategy = GenerationType.IDENTITY) public Long id;
  @JsonIgnore @ManyToOne(optional = false) @JoinColumn(name = "discussion_id") public Discussion discussion;
  @ManyToOne(optional = false) @JoinColumn(name = "actor_id") public User actor;
  @Enumerated(EnumType.STRING) @Column(nullable = false) public Type type;
  @Column(nullable = false, length = 100) public String label;
  @Column(nullable = false, length = 1000) public String url;
  @Column(name = "created_at", nullable = false) public Instant createdAt = Instant.now();

  public enum Type { JIRA }
}
