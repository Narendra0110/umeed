-- Photos of a donation. The file itself lives in storage; this table records where.
CREATE TABLE donation_images (
    id           UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    donation_id  UUID          NOT NULL,
    storage_key  VARCHAR(300)  NOT NULL,
    sort_order   INTEGER       NOT NULL,
    created_at   TIMESTAMPTZ   NOT NULL DEFAULT now(),
    CONSTRAINT fk_donation_images_donation FOREIGN KEY (donation_id) REFERENCES donations (id),
    CONSTRAINT uq_donation_images_storage_key UNIQUE (storage_key),
    CONSTRAINT uq_donation_images_order UNIQUE (donation_id, sort_order),
    CONSTRAINT ck_donation_images_sort_order CHECK (sort_order BETWEEN 0 AND 4)
);
