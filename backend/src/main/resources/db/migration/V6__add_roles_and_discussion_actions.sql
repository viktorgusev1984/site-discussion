ALTER TABLE users DROP CONSTRAINT users_role_check;
ALTER TABLE users ADD CONSTRAINT users_role_check CHECK (role IN ('USER', 'MODERATOR', 'ADMIN'));

ALTER TABLE discussions DROP CONSTRAINT discussions_status_check;
ALTER TABLE discussions ADD CONSTRAINT discussions_status_check
    CHECK (status IN ('OPEN', 'PLANNED', 'DONE', 'CLOSED', 'CANCELLED'));

CREATE TABLE discussion_actions (
    id BIGSERIAL PRIMARY KEY,
    discussion_id BIGINT NOT NULL REFERENCES discussions(id) ON DELETE CASCADE,
    actor_id BIGINT NOT NULL REFERENCES users(id),
    type VARCHAR(20) NOT NULL CHECK (type IN ('JIRA')),
    label VARCHAR(100) NOT NULL,
    url VARCHAR(1000) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_discussion_actions_discussion ON discussion_actions(discussion_id, created_at);
