ALTER TABLE app_user
    ADD CONSTRAINT ck_app_user_username_format
        CHECK (username ~ '^[a-z0-9._-]{3,50}$');