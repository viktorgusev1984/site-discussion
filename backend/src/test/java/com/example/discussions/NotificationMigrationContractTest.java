package com.example.discussions;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class NotificationMigrationContractTest {
  @Test void rulesAreUniqueAndOwnedRowsCascadeWithTheUser() throws Exception {
    String sql = migration("V8__add_notification_channels_and_rules.sql");
    assertTrue(sql.contains("user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE"));
    assertTrue(sql.contains("UNIQUE NULLS NOT DISTINCT (user_id, trigger, scope, discussion_id, channel_id)"));
    assertTrue(sql.contains("channel_id BIGINT NOT NULL REFERENCES notification_channels(id) ON DELETE CASCADE"));
  }

  @Test void outboxIsIdempotentAndClaimQueryUsesSkipLocked() throws Exception {
    String sql = migration("V9__add_notification_delivery_outbox.sql");
    assertTrue(sql.contains("idempotency_key VARCHAR(200) NOT NULL UNIQUE"));
    assertTrue(sql.contains("recipient_user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE"));
    String repository = source("repository/NotificationDeliveryRepository.java");
    assertTrue(repository.contains("FOR UPDATE SKIP LOCKED"));
    assertTrue(repository.contains("ORDER BY next_attempt_at, id LIMIT 1"));
  }

  private String migration(String name) throws Exception {
    try (var input=getClass().getResourceAsStream("/db/migration/"+name)) {
      assertNotNull(input); return new String(input.readAllBytes(), StandardCharsets.UTF_8);
    }
  }
  private String source(String name) throws Exception {
    var path=java.nio.file.Path.of("src/main/java/com/example/discussions/"+name);
    return java.nio.file.Files.readString(path);
  }
}
