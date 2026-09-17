package com.example.discussions.repository;

import com.example.discussions.model.*;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CommentVoteRepository extends JpaRepository<CommentVote, Long> {
  Optional<CommentVote> findByUserAndComment(User user, Comment comment);
  long countByComment(Comment comment);
}
