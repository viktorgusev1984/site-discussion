-- Administrator account for testing moderation flows. The password is "admin12345".
-- This account is intentionally seeded only for the demo application.
INSERT INTO users (username, email, password_hash, display_name, bio, role)
VALUES (
    'admin',
    'admin@openideas.local',
    '$2a$10$3ycYHScHFDsyUSo3rh/0BuNS0VmRfkB.zpM2Xpsrn8tqILeubOFTy',
    'Демо-администратор',
    'Тестовый администратор для проверки модерации и управления ролями.',
    'ADMIN'
)
ON CONFLICT DO NOTHING;
