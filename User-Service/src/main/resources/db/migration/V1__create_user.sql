CREATE TABLE users (
                       id UUID NOT NULL,

                       email VARCHAR(255) NOT NULL,
                       password VARCHAR(255) NOT NULL,
                       phone_number VARCHAR(20) NOT NULL,

                       status VARCHAR(50) NOT NULL,
                       role VARCHAR(50) NOT NULL,

                       created_at TIMESTAMP NOT NULL,
                       updated_at TIMESTAMP NOT NULL,

                       CONSTRAINT pk_users PRIMARY KEY (id),
                       CONSTRAINT uq_users_email UNIQUE (email),
                       CONSTRAINT uq_users_phone UNIQUE (phone_number)
);
