-- F05: items a user marked "Not interested". They no longer appear on that user's Home.
CREATE TABLE donation_skips (
    user_id      UUID         NOT NULL,
    donation_id  UUID         NOT NULL,
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT pk_donation_skips PRIMARY KEY (user_id, donation_id),
    CONSTRAINT fk_donation_skips_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_donation_skips_donation FOREIGN KEY (donation_id) REFERENCES donations (id)
);
