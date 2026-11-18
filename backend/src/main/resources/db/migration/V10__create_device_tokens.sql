-- F02: the phones a user is logged in on, for push notifications.
CREATE TABLE device_tokens (
    id          UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     UUID          NOT NULL,
    token       VARCHAR(512)  NOT NULL,
    platform    VARCHAR(20)   NOT NULL DEFAULT 'ANDROID',
    created_at  TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ   NOT NULL DEFAULT now(),
    CONSTRAINT fk_device_tokens_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT uq_device_tokens_token UNIQUE (token),
    CONSTRAINT ck_device_tokens_platform CHECK (platform IN ('ANDROID', 'IOS'))
);

CREATE INDEX idx_device_tokens_user_id ON device_tokens (user_id);
