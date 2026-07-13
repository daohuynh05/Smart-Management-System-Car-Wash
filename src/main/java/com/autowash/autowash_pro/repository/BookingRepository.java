package com.autowash.autowash_pro.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.autowash.autowash_pro.entity.Booking;
import com.autowash.autowash_pro.enums.BookingStatus;

public interface BookingRepository extends JpaRepository<Booking, UUID> {

    @EntityGraph(attributePaths = { "customer", "vehicle", "washHistory" })
    List<Booking> findByCustomer_CustomerId(UUID customerId);

    @EntityGraph(attributePaths = { "customer", "vehicle", "washHistory" })
    List<Booking> findByCustomer_CustomerIdOrderByScheduledAtDesc(UUID customerId);

    @EntityGraph(attributePaths = { "customer", "vehicle", "washHistory" })
    List<Booking> findByStatus(BookingStatus status);

    @EntityGraph(attributePaths = { "customer", "vehicle", "washHistory" })
    List<Booking> findByStatusOrderByScheduledAtAsc(BookingStatus status);

    @EntityGraph(attributePaths = { "customer", "vehicle", "washHistory" })
    List<Booking> findByCustomer_CustomerIdAndStatus(UUID customerId, BookingStatus status);

    int countByCustomer_CustomerIdAndStatus(UUID customerId, BookingStatus status);

    @EntityGraph(attributePaths = { "customer", "vehicle" })
    List<Booking> findByScheduledAtBetween(LocalDateTime from, LocalDateTime to);

    @EntityGraph(attributePaths = { "customer", "vehicle" })
    List<Booking> findByScheduledAtBetweenOrderByPriorityScoreDescCreatedAtAsc(LocalDateTime from, LocalDateTime to);

    @EntityGraph(attributePaths = { "customer", "vehicle", "washHistory" })
    List<Booking> findByStatusOrderByScheduledAtDesc(BookingStatus status);

    @EntityGraph(attributePaths = { "customer", "vehicle" })
    List<Booking> findByScheduledAtBetweenOrderByScheduledAtDesc(LocalDateTime from, LocalDateTime to);

    @EntityGraph(attributePaths = { "customer", "vehicle" })
    List<Booking> findAllByOrderByScheduledAtDesc();

    int countByScheduledAtAndStatusIn(LocalDateTime scheduledAt, List<BookingStatus> statuses);

    boolean existsByVehicle_VehicleId(UUID vehicleId);

    boolean existsByCustomer_CustomerIdAndPromotion_PromoIdAndStatusNot(UUID customerId, UUID promoId,
            BookingStatus status);

    @EntityGraph(attributePaths = { "bookingServices", "bookingServices.washService" })
    List<Booking> findByVehicle_VehicleIdAndStatusOrderByScheduledAtDesc(UUID vehicleId, BookingStatus status);
}