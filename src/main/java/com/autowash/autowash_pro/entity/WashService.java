package com.autowash.autowash_pro.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import org.hibernate.annotations.CreationTimestamp;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "services")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties({ "hibernateLazyInitializer", "handler" })
public class WashService {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "service_id", updatable = false, nullable = false)
    private UUID serviceId;

    @Column(name = "service_name", nullable = false, length = 100)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "base_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal basePrice;

    @Column(name = "estimated_duration", nullable = false)
    private Integer estimatedDuration;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private boolean isActive = true;

    @Column(name = "points", nullable = false)
    @Builder.Default
    private Integer points = 0;

    @Column(name = "is_combo", nullable = false)
    @Builder.Default
    private boolean isCombo = false;

    @Column(name = "bundled_service_ids", columnDefinition = "TEXT")
    private String bundledServiceIds;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
