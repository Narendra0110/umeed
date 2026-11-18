-- F01: blocking between users, and erasing private data when an account is deleted.

-- People a user has blocked. A block works both ways: neither person sees or claims the other's items.
CREATE TABLE user_blocks (
    blocker_id  UUID         NOT NULL,
    blocked_id  UUID         NOT NULL,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT pk_user_blocks PRIMARY KEY (blocker_id, blocked_id),
    CONSTRAINT fk_user_blocks_blocker FOREIGN KEY (blocker_id) REFERENCES users (id),
    CONSTRAINT fk_user_blocks_blocked FOREIGN KEY (blocked_id) REFERENCES users (id),
    CONSTRAINT ck_user_blocks_not_self CHECK (blocker_id <> blocked_id)
);

CREATE INDEX idx_user_blocks_blocked_id ON user_blocks (blocked_id);

-- A deleted account's pickup addresses are erased, so the column must allow empty values.
ALTER TABLE donations ALTER COLUMN pickup_address DROP NOT NULL;
