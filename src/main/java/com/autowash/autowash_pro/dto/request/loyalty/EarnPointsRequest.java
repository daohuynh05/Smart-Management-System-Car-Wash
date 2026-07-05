package com.autowash.autowash_pro.dto.request.loyalty;

import java.math.BigDecimal;
import java.util.UUID;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
public class EarnPointsRequest {

    @NotNull(message = "customerId không được để trống")
    private UUID customerId;

    @NotNull(message = "Số tiền thanh toán không được để trống")
    @Positive(message = "Số tiền thanh toán phải lớn hơn 0")
    private BigDecimal amountPaid;

    @NotNull(message = "washId không được để trống")
    private UUID washId;

    private Integer customPoints;
}
