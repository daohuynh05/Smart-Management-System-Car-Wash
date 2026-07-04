CREATE EXTENSION IF NOT EXISTS "pgcrypto";

CREATE TABLE customers (
    customer_id   UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    full_name     VARCHAR(100) NOT NULL,
    phone         VARCHAR(15)  UNIQUE NOT NULL,
    email         VARCHAR(100) UNIQUE,
    password      VARCHAR(255) NOT NULL,
    tier          VARCHAR(20)  NOT NULL DEFAULT 'MEMBER',
    total_points  INT          NOT NULL DEFAULT 0,
    lifetime_points INT        NOT NULL DEFAULT 0,
    total_visits  INT          NOT NULL DEFAULT 0,
    total_spend   DECIMAL(12,2) NOT NULL DEFAULT 0,
    registered_at TIMESTAMP    NOT NULL DEFAULT NOW(),
    last_visit_at TIMESTAMP,
    is_active     BOOLEAN      NOT NULL DEFAULT TRUE
);

CREATE TABLE vehicles (
    vehicle_id    UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    customer_id   UUID NOT NULL REFERENCES customers(customer_id) ON DELETE CASCADE,
    license_plate VARCHAR(20) UNIQUE NOT NULL,
    vehicle_type  VARCHAR(50) NOT NULL,
    brand         VARCHAR(50),
    color         VARCHAR(30),
    is_primary    BOOLEAN NOT NULL DEFAULT FALSE,
    created_at    TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE bookings (
    booking_id     UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    customer_id    UUID NOT NULL REFERENCES customers(customer_id),
    vehicle_id     UUID NOT NULL REFERENCES vehicles(vehicle_id),
    scheduled_at   TIMESTAMP NOT NULL,
    service_type   VARCHAR(50),
    status         VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    priority_score INT NOT NULL,
    notes          TEXT,
    created_at     TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE wash_history (
    wash_id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    customer_id      UUID NOT NULL REFERENCES customers(customer_id),
    vehicle_id       UUID NOT NULL REFERENCES vehicles(vehicle_id),
    booking_id       UUID REFERENCES bookings(booking_id),
    washed_at        TIMESTAMP NOT NULL DEFAULT NOW(),
    service_type     VARCHAR(50) NOT NULL,
    amount_paid      DECIMAL(10,2) NOT NULL,
    points_earned    INT NOT NULL DEFAULT 0,
    points_redeemed  INT NOT NULL DEFAULT 0,
    discount_applied DECIMAL(10,2) NOT NULL DEFAULT 0,
    lpr_detected     BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE TABLE customer_points (
    point_id      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    customer_id   UUID NOT NULL REFERENCES customers(customer_id),
    type          VARCHAR(20) NOT NULL,
    points        INT NOT NULL,
    balance_after INT NOT NULL,
    reference_id  UUID,
    description   VARCHAR(200),
    expires_at    TIMESTAMP NOT NULL,
    created_at    TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE promotions (
    promo_id    UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name        VARCHAR(100) NOT NULL,
    target_tiers VARCHAR(100) NOT NULL,
    promo_type  VARCHAR(30) NOT NULL,
    value       DECIMAL(10,2) NOT NULL,
    starts_at   TIMESTAMP NOT NULL,
    ends_at     TIMESTAMP NOT NULL,
    is_active   BOOLEAN NOT NULL DEFAULT TRUE,
    usage_limit INT,
    usage_count INT NOT NULL DEFAULT 0,
    created_at  TIMESTAMP NOT NULL DEFAULT NOW()
);

-- Indexes quan trọng cho performance
CREATE INDEX idx_bookings_customer   ON bookings(customer_id);
CREATE INDEX idx_bookings_status     ON bookings(status);
CREATE INDEX idx_bookings_scheduled  ON bookings(scheduled_at);
CREATE INDEX idx_points_customer     ON customer_points(customer_id);
CREATE INDEX idx_points_expires      ON customer_points(expires_at);
CREATE INDEX idx_vehicles_plate      ON vehicles(license_plate);