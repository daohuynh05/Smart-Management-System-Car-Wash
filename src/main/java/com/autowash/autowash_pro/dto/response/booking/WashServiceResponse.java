package com.autowash.autowash_pro.dto.response.booking;

import java.math.BigDecimal;
import java.util.UUID;

import com.autowash.autowash_pro.entity.WashService;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class WashServiceResponse {
    private UUID serviceId;
    private String name;
    private String description;
    private BigDecimal basePrice;
    private Integer estimatedDuration;
    private boolean isActive;
    private Integer points;
    private boolean isCombo;
    private String bundledServiceIds;

    public static WashServiceResponse from(WashService service) {
        if (service == null)
            return null;
        return WashServiceResponse.builder()
                .serviceId(service.getServiceId())
                .name(service.getName())
                .description(service.getDescription())
                .basePrice(service.getBasePrice())
                .estimatedDuration(service.getEstimatedDuration())
                .isActive(service.isActive())
                .points(service.getPoints())
                .isCombo(service.isCombo())
                .bundledServiceIds(service.getBundledServiceIds())
                .build();
    }
}
