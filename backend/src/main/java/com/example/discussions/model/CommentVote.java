package com.example.discussions.model;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "comment_votes", uniqueConstraints = @UniqueConstraint(name = "uk_comment_vote_user_comment", columnNames = {"user_id", "comment_id"}))
public class CommentVote {
  @Id @GeneratedValue(strategy = GenerationType.IDENTITY) public Long id;
  @ManyToOne(optional = false) @JoinColumn(name = "user_id") public User user;
  @ManyToOne(optional = false) @JoinColumn(name = "comment_id") public Comment comment;
  @Column(name = "created_at", nullable = false) public Instant createdAt = Instant.now();
}
