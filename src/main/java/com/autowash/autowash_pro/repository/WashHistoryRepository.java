package com.autowash.autowash_pro.repository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.autowash.autowash_pro.entity.WashHistory;
import com.autowash.autowash_pro.enums.PromoType;

import io.lettuce.core.dynamic.annotation.Param;
@Repository
public interface WashHistoryRepository
        extends JpaRepository<WashHistory, UUID> {

    List<WashHistory> findByCustomer_CustomerIdOrderByWashedAtDesc(
        UUID customerId);

    // Đếm lần rửa trong 12 tháng (dùng cho tier review)
    @Query("SELECT COUNT(w) FROM WashHistory w " +
           "WHERE w.customer.customerId = :customerId " +
           "AND w.washedAt >= :since")
    int countVisitsSince(
        @Param("customerId") UUID customerId,
        @Param("since") LocalDateTime since);

    boolean existsByBooking_BookingId(UUID bookingId);

    boolean existsByCustomer_CustomerIdAndPromo_PromoTypeAndWashedAtAfter(
        UUID customerId, PromoType promoType, LocalDateTime washedAt);
    List<WashHistory> findByWashedAtBetween(LocalDateTime start, LocalDateTime end);
}