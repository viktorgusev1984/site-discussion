package com.example.discussions.repository;

import com.example.discussions.model.NotificationChannel;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationChannelRepository extends JpaRepository<NotificationChannel, Long> {
  List<NotificationChannel> findByUserIdOrderByCreatedAtAsc(Long userId);
}
