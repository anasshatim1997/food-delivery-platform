CREATE TABLE IF NOT EXISTS users (
                                     id                              UUID         NOT NULL,
                                     email                           VARCHAR(255) NOT NULL,
    password                        VARCHAR(100),
    phone                           VARCHAR(20),
    status                          VARCHAR(50)  NOT NULL DEFAULT 'ACTIVE',
    role                            VARCHAR(50)  NOT NULL DEFAULT 'CUSTOMER',
    is_verified                     BOOLEAN      NOT NULL DEFAULT false,
    profile_completed               BOOLEAN      NOT NULL DEFAULT false,
    verification_code               VARCHAR(255),
    verification_code_expires_at    TIMESTAMP,
    password_reset_token            VARCHAR(255),
    password_reset_token_expires_at TIMESTAMP,
    oauth_provider                  VARCHAR(50),
    oauth_provider_id               VARCHAR(255),
    created_at                      TIMESTAMP    NOT NULL,
    updated_at                      TIMESTAMP    NOT NULL,

    CONSTRAINT pk_users         PRIMARY KEY (id),
    CONSTRAINT uq_users_email   UNIQUE (email),
    CONSTRAINT uq_users_phone   UNIQUE (phone),
    CONSTRAINT chk_users_role   CHECK (role IN ('USER', 'CUSTOMER', 'DRIVER', 'RESTAURANT_OWNER', 'ADMIN')),
    CONSTRAINT chk_users_status CHECK (status IN ('ACTIVE', 'SUSPENDED', 'DELETED'))
    );

CREATE INDEX IF NOT EXISTS idx_users_email          ON users (email);
CREATE INDEX IF NOT EXISTS idx_users_phone          ON users (phone);
CREATE INDEX IF NOT EXISTS idx_users_role           ON users (role);
CREATE INDEX IF NOT EXISTS idx_users_status         ON users (status);
CREATE INDEX IF NOT EXISTS idx_users_role_status    ON users (role, status);
CREATE INDEX IF NOT EXISTS idx_users_verification   ON users (verification_code);
CREATE INDEX IF NOT EXISTS idx_users_oauth          ON users (oauth_provider, oauth_provider_id);
CREATE UNIQUE INDEX IF NOT EXISTS idx_users_reset_token ON users (password_reset_token) WHERE password_reset_token IS NOT NULL;