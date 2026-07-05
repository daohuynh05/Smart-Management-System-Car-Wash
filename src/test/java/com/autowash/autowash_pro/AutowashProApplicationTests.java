package com.autowash.autowash_pro;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class AutowashProApplicationTests {

	@org.springframework.beans.factory.annotation.Autowired
	private org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;

	@Test
	void contextLoads() {
		System.out.println("=== DB TEST START ===");
		String sql = "INSERT INTO wash_history (wash_id, customer_id, vehicle_id, booking_id, washed_at, service_type, amount_paid, points_earned, points_redeemed, discount_applied, lpr_detected) " +
		             "SELECT gen_random_uuid(), b.customer_id, b.vehicle_id, b.booking_id, b.scheduled_at, COALESCE(b.service_type, 'BASIC'), COALESCE(b.total_amount, 50000), FLOOR(COALESCE(b.total_amount, 50000) / 5000), COALESCE(b.used_points, 0), COALESCE(b.discount_amount, 0) + COALESCE(b.points_discount_amount, 0), FALSE " +
		             "FROM bookings b WHERE b.status = 'DONE' AND NOT EXISTS (SELECT 1 FROM wash_history w WHERE w.booking_id = b.booking_id)";
		int updated = jdbcTemplate.update(sql);
		System.out.println("Backfilled wash_history records: " + updated);
		int washCount = jdbcTemplate.queryForObject("SELECT count(*) FROM wash_history", Integer.class);
		System.out.println("WashHistory Count after backfill: " + washCount);
		System.out.println("=== DB TEST END ===");
	}

}
