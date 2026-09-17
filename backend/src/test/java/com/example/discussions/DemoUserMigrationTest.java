package com.example.discussions;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

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

    @Test
    void seededAdministratorHasAdminRoleAndPublishedCredentials() throws Exception {
        try (var migration = getClass()
                .getResourceAsStream("/db/migration/V7__add_demo_admin.sql")) {
            var sql = new String(migration.readAllBytes(), StandardCharsets.UTF_8);
            var hashStart = sql.indexOf("'$2a$") + 1;
            var hashEnd = sql.indexOf("'", hashStart);
            var hash = sql.substring(hashStart, hashEnd);

            assertTrue(new BCryptPasswordEncoder().matches("admin12345", hash));
            assertTrue(sql.contains("'ADMIN'"));
            assertEquals(1, sql.split("'admin'", -1).length - 1);
        }
    }
}
