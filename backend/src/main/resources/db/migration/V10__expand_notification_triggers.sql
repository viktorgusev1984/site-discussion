ALTER TABLE notification_rules DROP CONSTRAINT IF EXISTS notification_rules_trigger_check;
ALTER TABLE notification_rules ADD CONSTRAINT notification_rules_trigger_check CHECK (
    trigger IN ('NEW_DISCUSSION', 'NEW_COMMENT', 'NEW_REPLY', 'FIRST_VOTE', 'NEW_REACTION',
                'STATUS_CHANGED', 'JIRA_ACTION_CREATED', 'MENTION')
);
