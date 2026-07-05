package com.autowash.autowash_pro.dto.response;

import com.autowash.autowash_pro.enums.PromoType;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PromotionResponse {
    private UUID promoId;
    private String name;
    private String description;
    private PromoType promoType;
    private BigDecimal value;
    private LocalDateTime endsAt;
}
