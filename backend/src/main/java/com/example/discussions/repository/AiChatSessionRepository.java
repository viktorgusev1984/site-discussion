package com.example.discussions.repository;

import com.example.discussions.model.AiChatSession;
import com.example.discussions.model.User;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AiChatSessionRepository extends JpaRepository<AiChatSession, Long> {
  Optional<AiChatSession> findFirstByUserOrderByCreatedAtDesc(User user);
  boolean existsBySessionIdAndUser(String sessionId, User user);
}
