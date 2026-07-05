package com.autowash.autowash_pro.dto.request.booking;

import com.autowash.autowash_pro.enums.BookingStatus;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateBookingStatusRequest {

    @NotNull(message = "status không được để trống")
    private BookingStatus status;

    private java.util.UUID promoId;
}
