package com.example.discussions.model;
import jakarta.persistence.*;import java.time.Instant;
@Entity @Table(name="votes",uniqueConstraints=@UniqueConstraint(name="uk_vote_user_discussion",columnNames={"user_id","discussion_id"})) public class Vote {@Id @GeneratedValue(strategy=GenerationType.IDENTITY) public Long id;@ManyToOne(optional=false) @JoinColumn(name="user_id") public User user;@ManyToOne(optional=false) @JoinColumn(name="discussion_id") public Discussion discussion;@Column(name="created_at",nullable=false) public Instant createdAt=Instant.now();}
