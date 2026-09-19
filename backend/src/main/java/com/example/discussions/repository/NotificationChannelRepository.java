package com.example.discussions.repository;

import com.example.discussions.model.NotificationChannel;
import java.util.List;
import java.time.Instant;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NotificationChannelRepository extends JpaRepository<NotificationChannel, Long> {
  List<NotificationChannel> findByUserIdOrderByCreatedAtAsc(Long userId);
  Optional<NotificationChannel> findByIdAndUserId(Long id, Long userId);

  @Modifying(clearAutomatically = true, flushAutomatically = true)
  @Query("""
      update NotificationChannel c set c.lastCheckAttemptAt = :now
      where c.id = :id and c.user.id = :userId
        and (c.lastCheckAttemptAt is null or c.lastCheckAttemptAt <= :cutoff)
      """)
  int claimCheck(@Param("id") Long id, @Param("userId") Long userId,
      @Param("now") Instant now, @Param("cutoff") Instant cutoff);
}
