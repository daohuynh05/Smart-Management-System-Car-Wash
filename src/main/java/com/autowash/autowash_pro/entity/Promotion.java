package com.autowash.autowash_pro.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;

import com.autowash.autowash_pro.enums.PromoType;
import com.autowash.autowash_pro.enums.Tier;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "promotions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Promotion {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "promo_id", updatable = false, nullable = false)
    private UUID promoId;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 255)
    private String description;

    // Lưu dạng "SILVER,GOLD,PLATINUM"
    @Column(name = "target_tiers", nullable = false, length = 100)
    private String targetTiers;

    @Enumerated(EnumType.STRING)
    @Column(name = "promo_type", nullable = false, length = 30)
    private PromoType promoType;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal value;

    @Column(name = "max_discount", precision = 10, scale = 2)
    private BigDecimal maxDiscount;

    @Column(name = "starts_at", nullable = false)
    private LocalDateTime startsAt;

    @Column(name = "ends_at", nullable = false)
    private LocalDateTime endsAt;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private boolean isActive = true;

    @Column(name = "usage_limit")
    private Integer usageLimit;

    @Column(name = "usage_count", nullable = false)
    @Builder.Default
    private int usageCount = 0;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // Helper: convert targetTiers string ↔ List<Tier>
    @Transient
    public List<Tier> getTargetTierList() {
        return List.of(targetTiers.split(","))
                .stream()
                .map(Tier::valueOf)
                .toList();
    }

    @Transient
    public void setTargetTierList(List<Tier> tiers) {
        this.targetTiers = tiers.stream()
                .map(Tier::name)
                .reduce((a, b) -> a + "," + b)
                .orElse("");
    }

    // Helper: kiểm tra còn hiệu lực không
    @Transient
    public boolean isValid() {
        LocalDateTime now = LocalDateTime.now();
        boolean withinTime = now.isAfter(startsAt) && now.isBefore(endsAt);
        boolean withinLimit = usageLimit == null || usageCount < usageLimit;
        return isActive && withinTime && withinLimit;
    }
}