-- A receiver's request for a donated item. The donor accepts at most one.
CREATE TABLE claims (
    id           UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    donation_id  UUID          NOT NULL,
    claimant_id  UUID          NOT NULL,
    message      VARCHAR(300),
    status       VARCHAR(20)   NOT NULL DEFAULT 'PENDING',
    created_at   TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_at   TIMESTAMPTZ   NOT NULL DEFAULT now(),
    CONSTRAINT fk_claims_donation FOREIGN KEY (donation_id) REFERENCES donations (id),
    CONSTRAINT fk_claims_claimant FOREIGN KEY (claimant_id) REFERENCES users (id),
    CONSTRAINT ck_claims_status CHECK (status IN ('PENDING', 'ACCEPTED', 'REJECTED', 'WITHDRAWN')),
    CONSTRAINT uq_claims_donation_claimant UNIQUE (donation_id, claimant_id)
);

-- A donation can never have two accepted claims, even if two requests arrive at the same moment.
CREATE UNIQUE INDEX uq_claims_one_accepted ON claims (donation_id) WHERE status = 'ACCEPTED';

CREATE INDEX idx_claims_claimant_created_at ON claims (claimant_id, created_at DESC);
