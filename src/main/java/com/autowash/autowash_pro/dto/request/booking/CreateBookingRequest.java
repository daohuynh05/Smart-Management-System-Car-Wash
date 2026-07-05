package com.autowash.autowash_pro.dto.request.booking;

import java.time.LocalDateTime;
import java.util.UUID;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateBookingRequest {

    @NotNull(message = "vehicleId không được để trống")
    private UUID vehicleId;

    @NotNull(message = "scheduledAt không được để trống")
    @Future(message = "Thời gian đặt lịch phải ở tương lai")
    @JsonDeserialize(using = LenientLocalDateTimeDeserializer.class)
    private LocalDateTime scheduledAt;

    private java.util.List<UUID> serviceIds;

    private UUID promoId;

    private int usedPoints;

    @Size(max = 1000, message = "Ghi chú tối đa 1000 ký tự")
    private String notes;
}
