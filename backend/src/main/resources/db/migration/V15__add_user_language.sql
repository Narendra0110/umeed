-- F07: the language the person chose for the app ('en' English, 'hi' Hindi). Notifications use it too.
ALTER TABLE users ADD COLUMN language VARCHAR(5) NOT NULL DEFAULT 'en';
ALTER TABLE users ADD CONSTRAINT ck_users_language CHECK (language IN ('en', 'hi'));
