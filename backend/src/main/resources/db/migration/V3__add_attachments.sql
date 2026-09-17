CREATE TABLE attachments (
    id UUID PRIMARY KEY,
    original_name VARCHAR(255) NOT NULL,
    content_type VARCHAR(50) NOT NULL,
    size BIGINT NOT NULL,
    data BYTEA NOT NULL,
    uploader_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_attachments_uploader ON attachments(uploader_id);
