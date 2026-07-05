package com.autowash.autowash_pro.dto.response.booking;

import java.math.BigDecimal;
import java.util.UUID;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class BookingServiceResponse {
    private UUID serviceId;
    private String serviceName;
    private int quantity;
    private BigDecimal unitPrice;
    private int duration;
    private BigDecimal subtotal;

    public static BookingServiceResponse from(com.autowash.autowash_pro.entity.BookingWashService item) {
        if (item == null) return null;
        return BookingServiceResponse.builder()
                .serviceId(item.getWashService().getServiceId())
                .serviceName(item.getWashService().getName())
                .quantity(item.getQuantity())
                .unitPrice(item.getUnitPrice())
                .duration(item.getDuration())
                .subtotal(item.getSubtotal())
                .build();
    }
}
