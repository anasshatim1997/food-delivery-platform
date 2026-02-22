CREATE TABLE IF NOT EXISTS drivers (
                                       id                  UUID           NOT NULL,
                                       user_id             UUID           NOT NULL,
                                       first_name          VARCHAR(255)   NOT NULL,
    last_name           VARCHAR(255)   NOT NULL,
    profile_image       VARCHAR(500),
    license_image       VARCHAR(500),
    vehicle_type        VARCHAR(50)    NOT NULL,
    vehicle_number      VARCHAR(50)    NOT NULL,
    license_number      VARCHAR(50)    NOT NULL,
    is_available        BOOLEAN        NOT NULL DEFAULT false,
    current_lat         DECIMAL(10, 8),
    current_lng         DECIMAL(11, 8),
    rating              DECIMAL(3, 2)  NOT NULL DEFAULT 0.00,
    total_deliveries    INTEGER        NOT NULL DEFAULT 0,
    wallet_balance      DECIMAL(19, 2) NOT NULL DEFAULT 0.00,
    verification_status VARCHAR(50)    NOT NULL DEFAULT 'PENDING',
    created_at          TIMESTAMP,

    CONSTRAINT pk_drivers               PRIMARY KEY (id),
    CONSTRAINT uq_drivers_user_id       UNIQUE (user_id),
    CONSTRAINT fk_drivers_user          FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT chk_drivers_vehicle_type CHECK (vehicle_type IN ('BIKE', 'SCOOTER', 'CAR')),
    CONSTRAINT chk_drivers_ver_status   CHECK (verification_status IN ('PENDING', 'APPROVED', 'REJECTED')),
    CONSTRAINT chk_drivers_rating       CHECK (rating >= 0.00 AND rating <= 5.00),
    CONSTRAINT chk_drivers_lat          CHECK (current_lat IS NULL OR (current_lat >= -90.0  AND current_lat <= 90.0)),
    CONSTRAINT chk_drivers_lng          CHECK (current_lng IS NULL OR (current_lng >= -180.0 AND current_lng <= 180.0))
    );

CREATE TABLE IF NOT EXISTS driver_verification_documents (
                                                             driver_id     UUID         NOT NULL,
                                                             document_type VARCHAR(100) NOT NULL,
    document_url  VARCHAR(500) NOT NULL,

    CONSTRAINT pk_driver_docs        PRIMARY KEY (driver_id, document_type),
    CONSTRAINT fk_driver_docs_driver FOREIGN KEY (driver_id) REFERENCES drivers (id) ON DELETE CASCADE
    );

CREATE INDEX IF NOT EXISTS idx_drivers_user_id    ON drivers (user_id);
CREATE INDEX IF NOT EXISTS idx_drivers_available  ON drivers (is_available);
CREATE INDEX IF NOT EXISTS idx_drivers_ver_status ON drivers (verification_status);
CREATE INDEX IF NOT EXISTS idx_drivers_assignment ON drivers (is_available, verification_status);
CREATE INDEX IF NOT EXISTS idx_drivers_location   ON drivers (current_lat, current_lng);
CREATE INDEX IF NOT EXISTS idx_drivers_rating     ON drivers (rating);
CREATE INDEX IF NOT EXISTS idx_drivers_vehicle    ON drivers (vehicle_type);