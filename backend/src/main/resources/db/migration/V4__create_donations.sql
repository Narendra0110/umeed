-- Items offered for free. The pickup address is private and is never returned in public responses.
CREATE TABLE donations (
    id              UUID             PRIMARY KEY DEFAULT gen_random_uuid(),
    donor_id        UUID             NOT NULL,
    category_id     UUID             NOT NULL,
    title           VARCHAR(100)     NOT NULL,
    description     VARCHAR(1000)    NOT NULL,
    item_condition  VARCHAR(20)      NOT NULL,
    status          VARCHAR(20)      NOT NULL DEFAULT 'AVAILABLE',
    city            VARCHAR(100)     NOT NULL,
    area            VARCHAR(100)     NOT NULL,
    latitude        DOUBLE PRECISION NOT NULL,
    longitude       DOUBLE PRECISION NOT NULL,
    pickup_address  VARCHAR(500)     NOT NULL,
    created_at      TIMESTAMPTZ      NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ      NOT NULL DEFAULT now(),
    CONSTRAINT fk_donations_donor FOREIGN KEY (donor_id) REFERENCES users (id),
    CONSTRAINT fk_donations_category FOREIGN KEY (category_id) REFERENCES categories (id),
    CONSTRAINT ck_donations_item_condition CHECK (item_condition IN ('NEW', 'LIKE_NEW', 'GOOD', 'USABLE')),
    CONSTRAINT ck_donations_status CHECK (status IN ('AVAILABLE', 'RESERVED', 'COMPLETED', 'REMOVED')),
    CONSTRAINT ck_donations_latitude CHECK (latitude BETWEEN -90 AND 90),
    CONSTRAINT ck_donations_longitude CHECK (longitude BETWEEN -180 AND 180)
);

CREATE INDEX idx_donations_status_created_at ON donations (status, created_at DESC);
CREATE INDEX idx_donations_donor_id ON donations (donor_id);
CREATE INDEX idx_donations_category_id ON donations (category_id);
