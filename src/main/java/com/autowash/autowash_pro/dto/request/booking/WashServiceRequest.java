package com.autowash.autowash_pro.dto.request.booking;

import java.math.BigDecimal;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class WashServiceRequest {
    private String name;
    private String description;
    private BigDecimal basePrice;
    private Integer estimatedDuration;
    private boolean isActive;
    private Integer points;
    private boolean isCombo;
    private String bundledServiceIds;
}
