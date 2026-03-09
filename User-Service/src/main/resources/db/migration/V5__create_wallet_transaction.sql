CREATE TABLE wallet_transactions (
                                     id UUID NOT NULL,
                                     user_id UUID NOT NULL,
                                     amount DECIMAL(19,2) NOT NULL,
                                     type VARCHAR(20) NOT NULL,
                                     description VARCHAR(500),
                                     reference_type VARCHAR(50),
                                     reference_id UUID,
                                     created_at TIMESTAMP NOT NULL,
                                     CONSTRAINT pk_wallet_transactions PRIMARY KEY (id),
                                     CONSTRAINT fk_wallet_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
                                     CONSTRAINT chk_transaction_type CHECK (type IN ('CREDIT', 'DEBIT'))
);

CREATE INDEX idx_wallet_user_created ON wallet_transactions (user_id, created_at DESC);
CREATE INDEX idx_wallet_reference ON wallet_transactions (reference_type, reference_id);