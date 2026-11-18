-- F06: reports can point at a need instead of a listing, and remember who reviewed them.
ALTER TABLE reports ALTER COLUMN donation_id DROP NOT NULL;
ALTER TABLE reports ADD COLUMN need_id UUID;
ALTER TABLE reports ADD COLUMN reviewed_by UUID;
ALTER TABLE reports ADD COLUMN reviewed_at TIMESTAMPTZ;
ALTER TABLE reports ADD COLUMN review_note VARCHAR(500);
ALTER TABLE reports ADD CONSTRAINT fk_reports_need FOREIGN KEY (need_id) REFERENCES need_requests (id);
ALTER TABLE reports ADD CONSTRAINT fk_reports_reviewed_by FOREIGN KEY (reviewed_by) REFERENCES users (id);
ALTER TABLE reports ADD CONSTRAINT ck_reports_one_target CHECK ((donation_id IS NULL) <> (need_id IS NULL));

-- One report per person per need (the listing rule, uq_reports_donation_reporter, stays as it is).
CREATE UNIQUE INDEX uq_reports_need_reporter ON reports (need_id, reporter_id) WHERE need_id IS NOT NULL;

-- Every step an admin takes, for accountability.
CREATE TABLE admin_actions (
    id           UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    admin_id     UUID          NOT NULL,
    action       VARCHAR(30)   NOT NULL,
    target_type  VARCHAR(20)   NOT NULL,
    target_id    UUID          NOT NULL,
    note         VARCHAR(500),
    created_at   TIMESTAMPTZ   NOT NULL DEFAULT now(),
    CONSTRAINT fk_admin_actions_admin FOREIGN KEY (admin_id) REFERENCES users (id),
    CONSTRAINT ck_admin_actions_action CHECK (action IN (
        'REVIEW_REPORT', 'DISMISS_REPORT', 'REMOVE_DONATION', 'CLOSE_NEED', 'SUSPEND_USER', 'UNSUSPEND_USER')),
    CONSTRAINT ck_admin_actions_target_type CHECK (target_type IN ('REPORT', 'DONATION', 'NEED', 'USER'))
);

CREATE INDEX idx_admin_actions_created_at ON admin_actions (created_at DESC);
CREATE INDEX idx_admin_actions_target ON admin_actions (target_id);
