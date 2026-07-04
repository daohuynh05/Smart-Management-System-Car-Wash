package com.autowash.autowash_pro.dto.request.promotion;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class PromotionRequest {
    private String name;
    private String promoType;
    private BigDecimal value;
    private BigDecimal maxDiscount;
    private Integer usageLimit;
    private String targetTiers;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm")
    private LocalDateTime startsAt;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm")
    private LocalDateTime endsAt;
}
