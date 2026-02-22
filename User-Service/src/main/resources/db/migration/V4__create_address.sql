CREATE TABLE addresses (
                           id                    UUID           NOT NULL,
                           customer_id           UUID           NOT NULL,
                           label                 VARCHAR(50)    NOT NULL,
                           street                VARCHAR(255)   NOT NULL,
                           building              VARCHAR(100)   NOT NULL,
                           floor                 VARCHAR(50),
                           apartment             VARCHAR(50),
                           city                  VARCHAR(100)   NOT NULL,
                           latitude              DECIMAL(10, 8) NOT NULL,
                           longitude             DECIMAL(11, 8) NOT NULL,
                           delivery_instructions TEXT,
                           is_default            BOOLEAN        NOT NULL DEFAULT false,
                           created_at            TIMESTAMP      NOT NULL,
                           updated_at            TIMESTAMP      NOT NULL,

                           CONSTRAINT pk_addresses          PRIMARY KEY (id),
                           CONSTRAINT fk_addresses_customer FOREIGN KEY (customer_id) REFERENCES customers (user_id) ON DELETE CASCADE
);

CREATE INDEX idx_addresses_customer ON addresses (customer_id);
CREATE INDEX idx_addresses_default  ON addresses (customer_id, is_default);
CREATE INDEX idx_addresses_location ON addresses (latitude, longitude);
CREATE INDEX idx_addresses_city     ON addresses (city);
CREATE INDEX idx_addresses_label    ON addresses (customer_id, label);