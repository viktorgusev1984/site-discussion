CREATE TABLE comment_votes (
  id BIGSERIAL PRIMARY KEY,
  user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  comment_id BIGINT NOT NULL REFERENCES comments(id) ON DELETE CASCADE,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  CONSTRAINT uk_comment_vote_user_comment UNIQUE(user_id, comment_id)
);

CREATE TABLE reactions (
  id BIGSERIAL PRIMARY KEY,
  user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  discussion_id BIGINT REFERENCES discussions(id) ON DELETE CASCADE,
  comment_id BIGINT REFERENCES comments(id) ON DELETE CASCADE,
  emoji VARCHAR(16) NOT NULL,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  CONSTRAINT ck_reaction_target CHECK ((discussion_id IS NOT NULL) <> (comment_id IS NOT NULL)),
  CONSTRAINT uk_reaction_user_discussion_emoji UNIQUE(user_id, discussion_id, emoji),
  CONSTRAINT uk_reaction_user_comment_emoji UNIQUE(user_id, comment_id, emoji)
);

CREATE INDEX idx_comment_votes_comment ON comment_votes(comment_id);
CREATE INDEX idx_reactions_discussion ON reactions(discussion_id);
CREATE INDEX idx_reactions_comment ON reactions(comment_id);
