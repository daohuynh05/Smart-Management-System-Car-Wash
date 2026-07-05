package com.autowash.autowash_pro.dto.response.loyalty;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TierConfigResponse {
    private UUID configId;
    private BigDecimal vndPerPoint;
    private double multiplierMember;
    private double multiplierSilver;
    private double multiplierGold;
    private double multiplierPlatinum;
    private int pointExpiryMonths;
    private LocalDateTime updatedAt;
    private UUID updatedBy;
}
