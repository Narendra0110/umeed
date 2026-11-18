-- F03: private messages between a donor and the receiver whose claim was accepted.
-- A conversation belongs to one accepted claim, so a new receiver after a cancellation starts a new chat.
CREATE TABLE chat_messages (
    id          UUID           PRIMARY KEY DEFAULT gen_random_uuid(),
    claim_id    UUID           NOT NULL,
    sender_id   UUID           NOT NULL,
    body        VARCHAR(1000)  NOT NULL,
    created_at  TIMESTAMPTZ    NOT NULL DEFAULT now(),
    read_at     TIMESTAMPTZ,
    CONSTRAINT fk_chat_messages_claim FOREIGN KEY (claim_id) REFERENCES claims (id),
    CONSTRAINT fk_chat_messages_sender FOREIGN KEY (sender_id) REFERENCES users (id),
    CONSTRAINT ck_chat_messages_body_not_blank CHECK (length(btrim(body)) > 0)
);

CREATE INDEX idx_chat_messages_claim_created ON chat_messages (claim_id, created_at, id);
