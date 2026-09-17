-- Shared account for the public Render demo. The password is "demo12345".
-- ON CONFLICT keeps upgrades safe if somebody already claimed the demo username or email.
INSERT INTO users (username, email, password_hash, display_name, bio)
VALUES (
    'demo',
    'demo@openideas.local',
    '$2b$10$zW6KQScpCPlahClYwIAMrOgMbKbwDi//wyfg.peYO1ND9ff3dQa8O',
    'Демо-пользователь',
    'Общий тестовый аккаунт для демо-версии Open Ideas.'
)
ON CONFLICT DO NOTHING;
