package com.example.discussions;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

class DemoUserMigrationTest {
    @Test
    void seededPasswordMatchesPublishedDemoCredentials() throws Exception {
        try (var migration = getClass()
                .getResourceAsStream("/db/migration/V2__add_demo_user.sql")) {
            var sql = new String(migration.readAllBytes(), StandardCharsets.UTF_8);
            var hashStart = sql.indexOf("'$2b$") + 1;
            var hashEnd = sql.indexOf("'", hashStart);
            var hash = sql.substring(hashStart, hashEnd);

            assertTrue(new BCryptPasswordEncoder().matches("demo12345", hash));
        }
    }
}
