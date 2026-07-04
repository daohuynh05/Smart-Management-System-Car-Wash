ALTER TABLE bookings ADD COLUMN promo_id UUID REFERENCES promotions(promo_id);
ALTER TABLE bookings ADD COLUMN discount_amount DECIMAL(12,2) DEFAULT 0;
