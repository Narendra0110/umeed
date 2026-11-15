-- Handover confirmation: each side confirms separately; the donation is completed when both have.
ALTER TABLE donations
    ADD COLUMN donor_confirmed_at    TIMESTAMPTZ,
    ADD COLUMN receiver_confirmed_at TIMESTAMPTZ;

ALTER TABLE donations
    ADD CONSTRAINT ck_donations_completed_confirmed CHECK (
        status <> 'COMPLETED'
        OR (donor_confirmed_at IS NOT NULL AND receiver_confirmed_at IS NOT NULL)
    );
