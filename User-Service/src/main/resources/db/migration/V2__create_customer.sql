CREATE TABLE customers (
                           user_id UUID NOT NULL,
                           first_name VARCHAR(255) NOT NULL,
                           last_name VARCHAR(255) NOT NULL,
                           profile_image VARCHAR(500),
                           wallet_balance DECIMAL(19, 2) NOT NULL DEFAULT 0.00,
                           total_orders INTEGER NOT NULL DEFAULT 0,
                           CONSTRAINT pk_customers PRIMARY KEY (user_id),
                           CONSTRAINT fk_customers_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);