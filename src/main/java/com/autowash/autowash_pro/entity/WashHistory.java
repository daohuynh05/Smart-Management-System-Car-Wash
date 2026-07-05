package com.autowash.autowash_pro.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import com.autowash.autowash_pro.enums.ServiceType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "wash_history")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WashHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "wash_id", updatable = false, nullable = false)
    private UUID washId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vehicle_id", nullable = false)
    private Vehicle vehicle;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_id")
    private Booking booking;

    @Column(name = "washed_at", nullable = false)
    @Builder.Default
    private LocalDateTime washedAt = LocalDateTime.now();

    @Enumerated(EnumType.STRING)
    @Column(name = "service_type", nullable = false, length = 20)
    private ServiceType serviceType;

    @Column(name = "amount_paid",
            nullable = false, precision = 10, scale = 2)
    private BigDecimal amountPaid;

    @Column(name = "points_earned", nullable = false)
    @Builder.Default
    private int pointsEarned = 0;

    @Column(name = "points_redeemed", nullable = false)
    @Builder.Default
    private int pointsRedeemed = 0;

    @Column(name = "discount_applied",
            nullable = false, precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal discountApplied = BigDecimal.ZERO;

    @Column(name = "lpr_detected", nullable = false)
    @Builder.Default
    private boolean lprDetected = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "promo_id")
    private Promotion promo;
}