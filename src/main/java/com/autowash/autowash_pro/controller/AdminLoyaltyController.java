package com.autowash.autowash_pro.controller;

import com.autowash.autowash_pro.dto.request.loyalty.UpdateTierConfigRequest;
import com.autowash.autowash_pro.dto.response.loyalty.TierConfigResponse;
import com.autowash.autowash_pro.service.LoyaltyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin - Loyalty Config", description = "Cấu hình tỷ lệ điểm thưởng")
public class AdminLoyaltyController {

    private final LoyaltyService loyaltyService;

    // =========================================================================
    // GET /api/admin/tier-config — Xem cấu hình hiện tại
    // =========================================================================

    @GetMapping("/tier-config")
    @Operation(summary = "Xem cấu hình tỷ lệ điểm thưởng đang áp dụng")
    public ResponseEntity<TierConfigResponse> getTierConfig() {
        return ResponseEntity.ok(loyaltyService.getTierConfig());
    }

    // =========================================================================
    // PUT /api/admin/tier-config — Cập nhật cấu hình
    // =========================================================================

    @PutMapping("/tier-config")
    @Operation(summary = "Cập nhật cấu hình tỷ lệ điểm thưởng", description = """
            Admin có thể chỉnh:
            - vndPerPoint: số VND cần để tích 1 điểm (mặc định 10,000)
            - multiplierMember/Silver/Gold/Platinum: hệ số nhân điểm theo tier
            - pointExpiryMonths: số tháng trước khi điểm hết hạn (mặc định 12)
            """)
    public ResponseEntity<TierConfigResponse> updateTierConfig(
            @RequestBody @Valid UpdateTierConfigRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {

        return ResponseEntity.ok(
                loyaltyService.updateTierConfig(request, userDetails.getUsername()));
    }
}
