package com.autowash.autowash_pro.entity;

import java.time.LocalDateTime;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;

import com.autowash.autowash_pro.enums.BookingStatus;
import com.autowash.autowash_pro.enums.ServiceType;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import jakarta.persistence.CascadeType;
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
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "bookings")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties({ "hibernateLazyInitializer", "handler" })
public class Booking {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "booking_id", updatable = false, nullable = false)
    private UUID bookingId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    @JsonIgnoreProperties({ "bookings", "vehicles", "points" }) // Tránh lặp vô hạn vòng lặp quan hệ chéo
    private Customer customer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vehicle_id", nullable = false)
    @JsonIgnoreProperties("customer")
    private Vehicle vehicle;

    @Column(name = "scheduled_at", nullable = false)
    private LocalDateTime scheduledAt;

    // Logic cũ dùng enum nhx logic mới đã có thêm dịch vụ nên hông dùng cái này nữa
    // vẫn để vì database đã thêm xóa sẽ dẫn đến lỗi nên vẫn để chỉ là không dùng
    @Enumerated(EnumType.STRING)
    @Column(name = "service_type", nullable = true, length = 20)
    private ServiceType serviceType;

    @OneToMany(mappedBy = "booking", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    @JsonIgnoreProperties("booking")
    private java.util.List<BookingWashService> bookingServices = new java.util.ArrayList<>();

    @Column(name = "total_amount", precision = 12, scale = 2)
    private java.math.BigDecimal totalAmount;

    // thêm mới áp dựng promotion vào booking
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "promo_id")
    @JsonIgnoreProperties({ "hibernateLazyInitializer", "handler" })
    private Promotion promotion;

    @Column(name = "discount_amount", precision = 12, scale = 2)
    @Builder.Default
    private java.math.BigDecimal discountAmount = java.math.BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private BookingStatus status = BookingStatus.PENDING;

    @Column(name = "priority_score", nullable = false)
    private int priorityScore;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "used_points", nullable = false)
    @Builder.Default
    private int usedPoints = 0;

    @Column(name = "points_discount_amount", precision = 12, scale = 2)
    @Builder.Default
    private java.math.BigDecimal pointsDiscountAmount = java.math.BigDecimal.ZERO;
    // phía trên xử lý usedpoint và point discount là số tiền trừ khi dùng điểm á
    // Relationship ngược lại với WashHistory
    @OneToOne(mappedBy = "booking", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonIgnoreProperties("booking")
    private WashHistory washHistory;
}