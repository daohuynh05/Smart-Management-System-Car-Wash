package com.autowash.autowash_pro.dto.request.loyalty;

import java.util.UUID;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
public class RedeemPointsRequest {

    @NotNull(message = "customerId không được để trống")
    private UUID customerId;

    @NotNull(message = "Số điểm cần đổi không được để trống")
    @Min(value = 1, message = "Số điểm cần đổi phải lớn hơn 0")
    private Integer points;

    @NotNull(message = "referenceId không được để trống")
    private UUID referenceId;
}
