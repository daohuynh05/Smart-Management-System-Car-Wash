ALTER TABLE wash_history ADD COLUMN promo_id UUID REFERENCES promotions(promo_id);
