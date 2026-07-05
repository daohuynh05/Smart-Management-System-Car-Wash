package com.autowash.autowash_pro.controller;

import com.autowash.autowash_pro.entity.Reward;
import com.autowash.autowash_pro.entity.SystemConfig;
import com.autowash.autowash_pro.service.AdminConfigService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/config")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin - System Configuration", description = "Quản lý Cấu hình Hệ thống & Hạng Thẻ")
public class AdminConfigController {

    private final AdminConfigService adminConfigService;

    @GetMapping
    @Operation(summary = "Lấy toàn bộ cấu hình hệ thống")
    public ResponseEntity<SystemConfig> getSystemConfig() {
        return ResponseEntity.ok(adminConfigService.getSystemConfig());
    }

    @PutMapping
    @Operation(summary = "Lưu cập nhật cấu hình hệ thống")
    public ResponseEntity<Map<String, String>> updateSystemConfig(@RequestBody SystemConfig newConfig) {
        adminConfigService.updateSystemConfig(newConfig);
        Map<String, String> response = new HashMap<>();
        response.put("status", "SUCCESS");
        response.put("message", "Cấu hình hệ thống đã được cập nhật thành công!");
        return ResponseEntity.ok(response);
    }

    @PostMapping("/rewards")
    @Operation(summary = "Thêm phần thưởng mới")
    public ResponseEntity<Reward> addReward(@RequestBody Reward reward) {
        return ResponseEntity.status(HttpStatus.CREATED).body(adminConfigService.addReward(reward));
    }

    @DeleteMapping("/rewards/{id}")
    @Operation(summary = "Xóa phần thưởng")
    public ResponseEntity<Map<String, String>> deleteReward(@PathVariable Long id) {
        adminConfigService.deleteReward(id);
        Map<String, String> response = new HashMap<>();
        response.put("message", "Đã xóa phần thưởng thành công!");
        return ResponseEntity.ok(response);
    }
}
