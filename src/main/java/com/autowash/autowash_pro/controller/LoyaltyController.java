package com.autowash.autowash_pro.controller;

import com.autowash.autowash_pro.dto.request.loyalty.EarnPointsRequest;
import com.autowash.autowash_pro.dto.request.loyalty.RedeemPointsRequest;
import com.autowash.autowash_pro.dto.response.loyalty.*;
import com.autowash.autowash_pro.service.LoyaltyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@Tag(name = "Loyalty", description = "Quản lý điểm thưởng")
public class LoyaltyController {

    private final LoyaltyService loyaltyService;

    // =========================================================================
    // POST /api/loyalty/earn — Tích điểm (Admin trigger sau khi booking DONE)
    // =========================================================================

    @PostMapping("/api/loyalty/earn")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Tích điểm sau khi rửa xe xong")
    public ResponseEntity<EarnPointsResponse> earnPoints(
            @RequestBody @Valid EarnPointsRequest request) {
        return ResponseEntity.ok(loyaltyService.earnPoints(request));
    }

    // =========================================================================
    // POST /api/loyalty/redeem — Đổi điểm lấy thưởng
    // =========================================================================

    @PostMapping("/api/loyalty/redeem")
    @PreAuthorize("hasAnyRole('ADMIN', 'CUSTOMER')")
    @Operation(summary = "Trừ/Đổi điểm tích lũy của khách hàng")
    public ResponseEntity<RedeemPointsResponse> redeemPoints(
            @RequestBody @Valid RedeemPointsRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(
            loyaltyService.redeemPoints(request, userDetails.getUsername())
        );
    }

    // =========================================================================
    // GET /api/loyalty/balance/{customerId} — Xem số dư điểm (Client)
    // =========================================================================

    @GetMapping("/api/loyalty/balance/{customerId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'CUSTOMER')")
    @Operation(summary = "Xem số dư điểm và điểm sắp hết hạn")
    public ResponseEntity<PointBalanceResponse> getBalance(
            @PathVariable UUID customerId,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(
            loyaltyService.getBalance(customerId, userDetails.getUsername())
        );
    }

    // =========================================================================
    // POST /api/loyalty/tier-review — Trigger rà soát tier (Admin)
    // =========================================================================

    @PostMapping("/api/loyalty/tier-review")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Trigger rà soát và cập nhật tier thủ công")
    public ResponseEntity<Map<String, String>> triggerTierReview() {
        loyaltyService.runTierReview();
        return ResponseEntity.ok(Map.of("message", "Rà soát tier hoàn thành"));
    }

    // =========================================================================
    // GET /api/customers/{id}/points — Lịch sử điểm (Client)
    // =========================================================================

    @GetMapping("/api/customers/{customerId}/points")
    @PreAuthorize("hasAnyRole('ADMIN', 'CUSTOMER')")
    @Operation(summary = "Xem lịch sử giao dịch điểm (earn / redeem / expire)")
    public ResponseEntity<Page<PointHistoryResponse>> getPointHistory(
            @PathVariable UUID customerId,
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(
            loyaltyService.getPointHistory(customerId, pageable, userDetails.getUsername())
        );
    }
}
