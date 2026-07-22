CREATE TABLE app_user(
    id              UUID            NOT NULL,
    username        VARCHAR(50)     NOT NULL,
    password_hash   VARCHAR(255)    NOT NULL,
    role            VARCHAR(15)     NOT NULL,
    status          VARCHAR(15)     NOT NULL,
    version         BIGINT          NOT NULL DEFAULT 0,

    CONSTRAINT pk_app_user          PRIMARY KEY (id),
    CONSTRAINT uk_app_user_username UNIQUE (username),
    CONSTRAINT ck_app_user_role     CHECK (role IN ('USER', 'ADMIN')),
    CONSTRAINT ck_app_user_status   CHECK (status IN ('ACTIVE', 'LOCKED', 'DISABLED'))
);
