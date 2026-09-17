package com.example.discussions.repository;

import com.example.discussions.model.*;
import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReactionRepository extends JpaRepository<Reaction, Long> {
  List<Reaction> findByDiscussion(Discussion discussion);
  List<Reaction> findByComment(Comment comment);
  Optional<Reaction> findByUserAndDiscussionAndEmoji(User user, Discussion discussion, String emoji);
  Optional<Reaction> findByUserAndCommentAndEmoji(User user, Comment comment, String emoji);
}
