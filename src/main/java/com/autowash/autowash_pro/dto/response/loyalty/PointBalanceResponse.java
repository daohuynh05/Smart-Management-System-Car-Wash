package com.autowash.autowash_pro.dto.response.loyalty;

import java.time.LocalDateTime;
import java.util.UUID;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PointBalanceResponse {
    private UUID customerId;
    private String fullName;
    private String tier;
    private int currentPoints;
    private int lifetimePoints;
    private int expiringPointsIn30Days;
    private LocalDateTime nearestExpiryDate;
    private int totalVisits;
    private String nextTier;
    private int remainingVisits;
    private int silverThreshold;
    private int goldThreshold;
    private int platinumThreshold;
}
