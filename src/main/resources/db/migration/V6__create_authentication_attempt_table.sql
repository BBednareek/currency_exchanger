CREATE TABLE authentication_attempt
(
    subject_key       VARCHAR(64)              NOT NULL,
    failure_count     INTEGER                  NOT NULL,
    window_started_at TIMESTAMP WITH TIME ZONE NOT NULL,
    blocked_until     TIMESTAMP WITH TIME ZONE,
    updated_at        TIMESTAMP WITH TIME ZONE NOT NULL,

    CONSTRAINT pk_authentication_attempt
        PRIMARY KEY (subject_key),

    CONSTRAINT chk_authentication_attempt_subject_key
        CHECK (subject_key ~ '^[0-9a-f]{64}$'),

    CONSTRAINT chk_authentication_attempt_failure_count
        CHECK (failure_count >= 1),

    CONSTRAINT chk_authentication_attempt_blocked_until
        CHECK (
            blocked_until IS NULL
            OR blocked_until > window_started_at
        )
);

CREATE INDEX idx_authentication_attempt_updated_at
    ON authentication_attempt (updated_at);
