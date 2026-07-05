-- V3: Add dynamic wash services, combos, and booking relations

CREATE TABLE IF NOT EXISTS services (
    service_id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    service_name        VARCHAR(100) NOT NULL,
    description         TEXT,
    base_price          DECIMAL(12,2) NOT NULL,
    estimated_duration  INT NOT NULL,
    is_active           BOOLEAN NOT NULL DEFAULT TRUE,
    points              INT NOT NULL DEFAULT 0,
    is_combo            BOOLEAN NOT NULL DEFAULT FALSE,
    bundled_service_ids TEXT,
    created_at          TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS booking_services (
    booking_service_id  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    booking_id          UUID NOT NULL REFERENCES bookings(booking_id) ON DELETE CASCADE,
    service_id          UUID NOT NULL REFERENCES services(service_id),
    quantity            INT NOT NULL DEFAULT 1,
    unit_price          DECIMAL(12,2) NOT NULL,
    duration            INT NOT NULL,
    subtotal            DECIMAL(12,2) NOT NULL
);

-- Thêm cột tổng tiền hóa đơn cho đặt lịch (nếu chưa tồn tại)
ALTER TABLE bookings ADD COLUMN IF NOT EXISTS total_amount DECIMAL(12,2);

-- Tạo indexes tối ưu hiệu năng truy vấn
CREATE INDEX IF NOT EXISTS idx_booking_services_booking ON booking_services(booking_id);
CREATE INDEX IF NOT EXISTS idx_booking_services_service ON booking_services(service_id);
