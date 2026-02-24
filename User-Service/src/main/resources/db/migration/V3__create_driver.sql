CREATE TABLE IF NOT EXISTS drivers (
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

    CONSTRAINT pk_drivers PRIMARY KEY (user_id),

    CONSTRAINT fk_drivers_user
    FOREIGN KEY (user_id)
    REFERENCES users (id)
    ON DELETE CASCADE,

    CONSTRAINT chk_drivers_vehicle_type
    CHECK (vehicle_type IN ('BIKE','SCOOTER','CAR')),

    CONSTRAINT chk_drivers_ver_status
    CHECK (verification_status IN ('PENDING','APPROVED','REJECTED')),

    CONSTRAINT chk_drivers_rating
    CHECK (rating >= 0.00 AND rating <= 5.00),

    CONSTRAINT chk_drivers_lat
    CHECK (current_lat IS NULL OR (current_lat >= -90 AND current_lat <= 90)),

    CONSTRAINT chk_drivers_lng
    CHECK (current_lng IS NULL OR (current_lng >= -180 AND current_lng <= 180))
    );
