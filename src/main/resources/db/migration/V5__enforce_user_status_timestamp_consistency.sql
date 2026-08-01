UPDATE app_user
SET disabled_at = updated_at
WHERE status = 'DISABLED'
  AND disabled_at IS NULL;

UPDATE app_user
SET disabled_at = NULL
WHERE status <> 'DISABLED'
  AND disabled_at IS NOT NULL;

ALTER TABLE app_user
    ADD CONSTRAINT ck_app_user_disabled_timestamp
        CHECK (
            (status = 'DISABLED' AND disabled_at IS NOT NULL)
                OR
            (status <> 'DISABLED' AND disabled_at IS NULL)
            )
    NOT VALID;

ALTER TABLE app_user
    VALIDATE CONSTRAINT ck_app_user_disabled_timestamp;
