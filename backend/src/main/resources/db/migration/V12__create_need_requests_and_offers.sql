-- F04: things people need, and the items donors offer for them.
CREATE TABLE need_requests (
    id            UUID             PRIMARY KEY DEFAULT gen_random_uuid(),
    requester_id  UUID             NOT NULL,
    category_id   UUID             NOT NULL,
    title         VARCHAR(100)     NOT NULL,
    description   VARCHAR(1000)    NOT NULL,
    status        VARCHAR(20)      NOT NULL DEFAULT 'OPEN',
    city          VARCHAR(100)     NOT NULL,
    area          VARCHAR(100)     NOT NULL,
    latitude      DOUBLE PRECISION NOT NULL,
    longitude     DOUBLE PRECISION NOT NULL,
    created_at    TIMESTAMPTZ      NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ      NOT NULL DEFAULT now(),
    CONSTRAINT fk_need_requests_requester FOREIGN KEY (requester_id) REFERENCES users (id),
    CONSTRAINT fk_need_requests_category FOREIGN KEY (category_id) REFERENCES categories (id),
    CONSTRAINT ck_need_requests_status CHECK (status IN ('OPEN', 'FULFILLED', 'CLOSED')),
    CONSTRAINT ck_need_requests_latitude CHECK (latitude BETWEEN -90 AND 90),
    CONSTRAINT ck_need_requests_longitude CHECK (longitude BETWEEN -180 AND 180)
);

CREATE INDEX idx_need_requests_status_created_at ON need_requests (status, created_at DESC);
CREATE INDEX idx_need_requests_requester_id ON need_requests (requester_id);

-- A donor offers one of their available items for a need. The requester accepts at most one.
CREATE TABLE need_offers (
    id           UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    need_id      UUID          NOT NULL,
    donation_id  UUID          NOT NULL,
    donor_id     UUID          NOT NULL,
    message      VARCHAR(300),
    status       VARCHAR(20)   NOT NULL DEFAULT 'PENDING',
    created_at   TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_at   TIMESTAMPTZ   NOT NULL DEFAULT now(),
    CONSTRAINT fk_need_offers_need FOREIGN KEY (need_id) REFERENCES need_requests (id),
    CONSTRAINT fk_need_offers_donation FOREIGN KEY (donation_id) REFERENCES donations (id),
    CONSTRAINT fk_need_offers_donor FOREIGN KEY (donor_id) REFERENCES users (id),
    CONSTRAINT ck_need_offers_status CHECK (status IN ('PENDING', 'ACCEPTED', 'DECLINED', 'WITHDRAWN')),
    CONSTRAINT uq_need_offers_need_donation UNIQUE (need_id, donation_id)
);

-- A need can never have two accepted offers, even if two requests arrive at the same moment.
CREATE UNIQUE INDEX uq_need_offers_one_accepted ON need_offers (need_id) WHERE status = 'ACCEPTED';
CREATE INDEX idx_need_offers_donor_created_at ON need_offers (donor_id, created_at DESC);
