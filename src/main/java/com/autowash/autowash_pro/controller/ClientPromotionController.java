package com.autowash.autowash_pro.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.autowash.autowash_pro.entity.Promotion;
import com.autowash.autowash_pro.service.PromotionService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/promotions")
@RequiredArgsConstructor
@Tag(name = "Promotion Client", description = "Xem danh sách khuyến mãi dành cho Khách hàng")
public class ClientPromotionController {

    private final PromotionService promotionService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'CUSTOMER')")
    @Operation(summary = "Lấy danh sách khuyến mãi đang hoạt động")
    public ResponseEntity<List<Promotion>> getActivePromotions() {
        // Lấy danh sách các khuyến mãi có trạng thái ACTIVE
        List<Promotion> activePromotions = promotionService.getPromotionsByStatus("ACTIVE", null);
        return ResponseEntity.ok(activePromotions);
    }
}
