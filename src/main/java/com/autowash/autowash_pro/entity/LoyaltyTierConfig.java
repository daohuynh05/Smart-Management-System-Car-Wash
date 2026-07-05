package com.autowash.autowash_pro.entity;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import jakarta.persistence.*;

import lombok.*;

@Entity
@Table(name = "loyalty_tier_config")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoyaltyTierConfig {
    @Id
    @GeneratedValue
    private UUID configId;

    private BigDecimal vndPerPoint;
    private double multiplierMember;
    private double multiplierSilver;
    private double multiplierGold;
    private double multiplierPlatinum;
    private int pointExpiryMonths;

    private boolean active;

    private LocalDateTime updatedAt;
    private UUID updatedBy;

    @PrePersist
    public void prePersist() {
        if (updatedAt == null) updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
