package com.autowash.autowash_pro.dto.response.loyalty;

import java.util.UUID;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class RedeemPointsResponse {
    private UUID customerId;
    private int pointsUsed;
    private int remainingBalance;
    private String message;
}
