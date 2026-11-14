-- Application users. Identity is proven by Firebase phone authentication.
CREATE TABLE users (
    id            UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    firebase_uid  VARCHAR(128),
    phone_number  VARCHAR(20),
    display_name  VARCHAR(80),
    role          VARCHAR(20)  NOT NULL DEFAULT 'USER',
    status        VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    deleted_at    TIMESTAMPTZ,
    CONSTRAINT uq_users_firebase_uid UNIQUE (firebase_uid),
    CONSTRAINT uq_users_phone_number UNIQUE (phone_number),
    CONSTRAINT ck_users_role CHECK (role IN ('USER', 'ADMIN')),
    CONSTRAINT ck_users_status CHECK (status IN ('ACTIVE', 'SUSPENDED', 'DELETED')),
    CONSTRAINT ck_users_phone_format CHECK (phone_number ~ '^\+[1-9][0-9]{7,14}$'),
    CONSTRAINT ck_users_identity_present CHECK (
        status = 'DELETED' OR (firebase_uid IS NOT NULL AND phone_number IS NOT NULL)
    )
);
