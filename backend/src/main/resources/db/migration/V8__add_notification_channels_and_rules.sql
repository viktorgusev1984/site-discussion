CREATE TABLE notification_channels (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    type VARCHAR(20) NOT NULL CHECK (type IN ('EMAIL', 'MATTERMOST', 'WEBHOOK')),
    name VARCHAR(100) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    configuration JSONB NOT NULL DEFAULT '{}'::jsonb,
    encrypted_secrets BYTEA,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    last_successful_check_at TIMESTAMPTZ
);

CREATE INDEX idx_notification_channels_user
    ON notification_channels(user_id);

COMMENT ON COLUMN notification_channels.configuration IS
    'Non-sensitive channel settings only; secrets belong in encrypted_secrets';
COMMENT ON COLUMN notification_channels.encrypted_secrets IS
    'Application-encrypted channel credentials and endpoint secrets';

CREATE TABLE notification_rules (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    trigger VARCHAR(40) NOT NULL CHECK (
        trigger IN ('NEW_DISCUSSION', 'NEW_COMMENT', 'STATUS_CHANGED', 'MENTION')
    ),
    scope VARCHAR(20) NOT NULL CHECK (scope IN ('ALL_DISCUSSIONS', 'DISCUSSION')),
    discussion_id BIGINT REFERENCES discussions(id) ON DELETE CASCADE,
    channel_id BIGINT NOT NULL REFERENCES notification_channels(id) ON DELETE CASCADE,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT ck_notification_rule_scope CHECK (
        (scope = 'ALL_DISCUSSIONS' AND discussion_id IS NULL)
        OR (scope = 'DISCUSSION' AND discussion_id IS NOT NULL)
    ),
    CONSTRAINT uk_notification_rule
        UNIQUE NULLS NOT DISTINCT (user_id, trigger, scope, discussion_id, channel_id)
);

CREATE INDEX idx_notification_rules_active_lookup
    ON notification_rules(user_id, trigger, discussion_id)
    WHERE active;
