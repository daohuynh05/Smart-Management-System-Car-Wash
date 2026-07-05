-- 1. Insert missing wash_history records for historical bookings that are in DONE status
INSERT INTO wash_history (
    wash_id, 
    customer_id, 
    vehicle_id, 
    booking_id, 
    washed_at, 
    service_type, 
    amount_paid, 
    points_earned, 
    points_redeemed, 
    discount_applied, 
    lpr_detected
)
SELECT 
    gen_random_uuid(),
    b.customer_id,
    b.vehicle_id,
    b.booking_id,
    b.scheduled_at,
    COALESCE(b.service_type, 'BASIC'),
    COALESCE(b.total_amount, 50000),
    FLOOR(COALESCE(b.total_amount, 50000) / 5000),
    COALESCE(b.used_points, 0),
    COALESCE(b.discount_amount, 0) + COALESCE(b.points_discount_amount, 0),
    FALSE
FROM bookings b
WHERE b.status = 'DONE'
  AND NOT EXISTS (
      SELECT 1 FROM wash_history w WHERE w.booking_id = b.booking_id
  );
