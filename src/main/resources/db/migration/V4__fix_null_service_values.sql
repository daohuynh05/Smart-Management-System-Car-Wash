-- V4: Fix missing columns and null values in services table

-- Thêm các cột mới nếu chưa tồn tại
ALTER TABLE services ADD COLUMN IF NOT EXISTS points INT DEFAULT 0;
ALTER TABLE services ADD COLUMN IF NOT EXISTS is_combo BOOLEAN DEFAULT FALSE;
ALTER TABLE services ADD COLUMN IF NOT EXISTS bundled_service_ids TEXT;

-- Cập nhật các dòng dữ liệu cũ bị NULL
UPDATE services SET is_combo = FALSE WHERE is_combo IS NULL;
UPDATE services SET points = 0 WHERE points IS NULL;

-- Thiết lập ràng buộc NOT NULL và giá trị mặc định cho cột
ALTER TABLE services ALTER COLUMN is_combo SET DEFAULT FALSE;
ALTER TABLE services ALTER COLUMN is_combo SET NOT NULL;

ALTER TABLE services ALTER COLUMN points SET DEFAULT 0;
ALTER TABLE services ALTER COLUMN points SET NOT NULL;
