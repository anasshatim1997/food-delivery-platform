CREATE TABLE users (
                       id UUID NOT NULL,
                       email VARCHAR(255) NOT NULL,
                       password VARCHAR(100),
                       phone VARCHAR(20),
                       status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',
                       role VARCHAR(50) NOT NULL DEFAULT 'USER',
                       is_verified BOOLEAN NOT NULL DEFAULT false,
                       verification_code VARCHAR(255),
                       verification_code_expires_at TIMESTAMP,
                       oauth_provider VARCHAR(50),
                       oauth_provider_id VARCHAR(255),
                       created_at TIMESTAMP NOT NULL,
                       updated_at TIMESTAMP NOT NULL,
                       CONSTRAINT pk_users PRIMARY KEY (id),
                       CONSTRAINT uq_users_email UNIQUE (email),
                       CONSTRAINT uq_users_phone UNIQUE (phone)
);

CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_phone ON users(phone);
CREATE INDEX idx_users_role ON users(role);
CREATE INDEX idx_users_status ON users(status);
CREATE INDEX idx_users_verification_code ON users(verification_code);
CREATE INDEX idx_users_oauth_provider ON users(oauth_provider, oauth_provider_id);