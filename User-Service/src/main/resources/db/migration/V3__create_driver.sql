CREATE TABLE drivers (
                         user_id UUID NOT NULL,
                         vehicle_type VARCHAR(50) NOT NULL,
                         vehicle_number VARCHAR(50) NOT NULL,
                         license_number VARCHAR(50) NOT NULL,
                         is_available BOOLEAN NOT NULL DEFAULT false,
                         current_lat DECIMAL(10, 8),
                         current_lng DECIMAL(11, 8),
                         rating DECIMAL(3, 2) NOT NULL DEFAULT 0.00,
                         total_deliveries INTEGER NOT NULL DEFAULT 0,
                         wallet_balance DECIMAL(19, 2) NOT NULL DEFAULT 0.00,
                         verification_status VARCHAR(50) NOT NULL,
                         CONSTRAINT pk_drivers PRIMARY KEY (user_id),
                         CONSTRAINT fk_drivers_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE TABLE driver_verification_documents (
                                               driver_id UUID NOT NULL,
                                               document_type VARCHAR(100) NOT NULL,
                                               document_url VARCHAR(500) NOT NULL,
                                               CONSTRAINT pk_driver_docs PRIMARY KEY (driver_id, document_type),
                                               CONSTRAINT fk_driver_docs_driver FOREIGN KEY (driver_id) REFERENCES drivers(user_id) ON DELETE CASCADE
);

CREATE INDEX idx_drivers_available ON drivers(is_available);
CREATE INDEX idx_drivers_verification ON drivers(verification_status);