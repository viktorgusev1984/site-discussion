-- Channel deletion is intentionally cascading: subscriptions and queued deliveries cannot outlive
-- the destination they reference. Constraint names make that policy explicit and inspectable.
ALTER TABLE notification_rules
    DROP CONSTRAINT notification_rules_channel_id_fkey,
    ADD CONSTRAINT fk_notification_rules_channel
        FOREIGN KEY (channel_id) REFERENCES notification_channels(id) ON DELETE CASCADE;

ALTER TABLE notification_deliveries
    DROP CONSTRAINT notification_deliveries_channel_id_fkey,
    ADD CONSTRAINT fk_notification_deliveries_channel
        FOREIGN KEY (channel_id) REFERENCES notification_channels(id) ON DELETE CASCADE;

ALTER TABLE notification_channels
    ADD COLUMN connection_description VARCHAR(255),
    ADD COLUMN last_check_attempt_at TIMESTAMPTZ;

-- Existing rows may have a URL in JSON from versions before channel management existed. Keep it
-- solely for delivery compatibility; API DTOs never serialize this internal column. Any later
-- settings update migrates the endpoint into encrypted_secrets and clears configuration.
UPDATE notification_channels
SET connection_description = CASE
    WHEN type = 'EMAIL' THEN 'configured email'
    ELSE 'configured endpoint'
END;

ALTER TABLE notification_channels
    ALTER COLUMN connection_description SET NOT NULL;
