package com.example.discussions.repository;

import com.example.discussions.model.NotificationDelivery;
import java.time.Instant;
import java.util.Optional;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface NotificationDeliveryRepository extends JpaRepository<NotificationDelivery, Long> {
  boolean existsByIdempotencyKey(String idempotencyKey);

  @Query(value = "SELECT * FROM notification_deliveries WHERE status = 'PENDING' AND next_attempt_at <= :now ORDER BY next_attempt_at, id LIMIT 1 FOR UPDATE SKIP LOCKED", nativeQuery = true)
  Optional<NotificationDelivery> findNextDueForUpdate(@Param("now") Instant now);
}
