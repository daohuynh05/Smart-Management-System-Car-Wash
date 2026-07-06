package com.autowash.autowash_pro.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.autowash.autowash_pro.entity.CustomerPoints;
import com.autowash.autowash_pro.enums.PointType;

public interface CustomerPointsRepository
                extends JpaRepository<CustomerPoints, UUID> {

        List<CustomerPoints> findByCustomer_CustomerIdOrderByCreatedAtDesc(
                        UUID customerId);

        // Pageable variant used by LoyaltyService
        org.springframework.data.domain.Page<CustomerPoints> findByCustomer_CustomerIdOrderByCreatedAtDesc(
                        UUID customerId, org.springframework.data.domain.Pageable pageable);

        // Tìm điểm sắp hết hạn trong khoảng (dùng cho view balance)
        @org.springframework.data.jpa.repository.Query("SELECT cp FROM CustomerPoints cp " +
                        "WHERE cp.customer.customerId = :customerId " +
                        "AND cp.type = com.autowash.autowash_pro.enums.PointType.EARN " +
                        "AND cp.expiresAt > :now AND cp.expiresAt <= :in30Days " +
                        "AND cp.points > 0 ORDER BY cp.expiresAt ASC")
        List<CustomerPoints> findExpiringEarnPoints(
                        @org.springframework.data.repository.query.Param("customerId") UUID customerId,
                        @org.springframework.data.repository.query.Param("now") LocalDateTime now,
                        @org.springframework.data.repository.query.Param("in30Days") LocalDateTime in30Days);

        // Active EARN points ordered by nearest expiry (FIFO consumption)
        @org.springframework.data.jpa.repository.Query("SELECT cp FROM CustomerPoints cp " +
                        "WHERE cp.customer.customerId = :customerId " +
                        "AND cp.type = com.autowash.autowash_pro.enums.PointType.EARN " +
                        "AND cp.points > 0 AND cp.expiresAt > :now " +
                        "ORDER BY cp.expiresAt ASC, cp.createdAt ASC")
        List<CustomerPoints> findActiveEarnPointsFifo(
                        @org.springframework.data.repository.query.Param("customerId") UUID customerId,
                        @org.springframework.data.repository.query.Param("now") LocalDateTime now);

        // Expired EARN points that still have points > 0
        @org.springframework.data.jpa.repository.Query("SELECT cp FROM CustomerPoints cp " +
                        "WHERE cp.type = :type " +
                        "AND cp.expiresAt <= :now " +
                        "AND cp.points > 0")
        List<CustomerPoints> findExpiredEarnPoints(
                        @org.springframework.data.repository.query.Param("type") PointType type,
                        @org.springframework.data.repository.query.Param("now") LocalDateTime now);

        // Tổng điểm EARN chưa hết hạn
        @org.springframework.data.jpa.repository.Query("SELECT COALESCE(SUM(cp.points), 0) FROM CustomerPoints cp " +
                        "WHERE cp.customer.customerId = :customerId " +
                        "AND cp.type = com.autowash.autowash_pro.enums.PointType.EARN " +
                        "AND cp.expiresAt > :now")
        int sumActivePoints(
                        @org.springframework.data.repository.query.Param("customerId") UUID customerId,
                        @org.springframework.data.repository.query.Param("now") LocalDateTime now);
}