-- Flyway Migration: V10__remove_legacy_config_tables.sql
-- Loại bỏ các bảng cấu hình tĩnh/dư thừa cũ sau khi gom logic cấu hình điểm thưởng và thăng hạng về system_configs và tier_rules.

DROP TABLE IF EXISTS loyalty_tier_config CASCADE;
DROP TABLE IF EXISTS tier_configs CASCADE;
