-- A user's report that a listing breaks the rules. Reviewed by hand during the MVP.
CREATE TABLE reports (
    id           UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    donation_id  UUID          NOT NULL,
    reporter_id  UUID          NOT NULL,
    reason       VARCHAR(30)   NOT NULL,
    details      VARCHAR(500),
    status       VARCHAR(20)   NOT NULL DEFAULT 'OPEN',
    created_at   TIMESTAMPTZ   NOT NULL DEFAULT now(),
    CONSTRAINT fk_reports_donation FOREIGN KEY (donation_id) REFERENCES donations (id),
    CONSTRAINT fk_reports_reporter FOREIGN KEY (reporter_id) REFERENCES users (id),
    CONSTRAINT ck_reports_reason CHECK (reason IN (
        'SELLING_OR_ASKING_MONEY', 'PROHIBITED_ITEM', 'FAKE_OR_MISLEADING', 'OFFENSIVE', 'OTHER')),
    CONSTRAINT ck_reports_status CHECK (status IN ('OPEN', 'REVIEWED', 'DISMISSED')),
    CONSTRAINT uq_reports_donation_reporter UNIQUE (donation_id, reporter_id)
);

CREATE INDEX idx_reports_status_created_at ON reports (status, created_at);
