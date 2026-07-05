// các entity lưu trên database trước đây trường amout bị trống null
// nên khi cộng total nó bị lỗi 0đ
// cái này để cập nhập lại trường trong đó nếu có booking thì thay không thì sẽ là mặc định 50000

-- 1. Backfill bookings total_amount where it is null (historical data)
UPDATE bookings b
SET total_amount = COALESCE(
    (SELECT SUM(bs.subtotal) FROM booking_services bs WHERE bs.booking_id = b.booking_id),
    50000
) - COALESCE(b.discount_amount, 0) - COALESCE(b.points_discount_amount, 0)
WHERE b.total_amount IS NULL;

-- 2. Sync customers total_spend and total_visits
UPDATE customers c
SET total_spend = COALESCE((
    SELECT SUM(b.total_amount)
    FROM bookings b
    WHERE b.customer_id = c.customer_id AND b.status = 'DONE'
), 0),
total_visits = COALESCE((
    SELECT COUNT(b.booking_id)
    FROM bookings b
    WHERE b.customer_id = c.customer_id AND b.status = 'DONE'
), 0);
