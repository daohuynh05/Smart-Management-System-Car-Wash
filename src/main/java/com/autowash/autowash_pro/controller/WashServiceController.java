package com.autowash.autowash_pro.controller;

import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import com.autowash.autowash_pro.dto.response.booking.WashServiceResponse;
import com.autowash.autowash_pro.repository.WashServiceRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

import org.springframework.web.bind.annotation.*;
import com.autowash.autowash_pro.dto.request.booking.WashServiceRequest;
import com.autowash.autowash_pro.entity.WashService;
import com.autowash.autowash_pro.exception.BusinessException;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@Tag(name = "Services", description = "Quản lý danh sách dịch vụ rửa xe")
public class WashServiceController {

    private final WashServiceRepository washServiceRepository;

    @GetMapping("/api/services")
    @Operation(summary = "Lấy danh sách các dịch vụ đang hoạt động")
    public ResponseEntity<List<WashServiceResponse>> getActiveServices() {
        List<WashServiceResponse> responses = washServiceRepository.findByIsActiveTrue().stream()
                .map(WashServiceResponse::from)
                .toList();
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/api/admin/services")
    @Operation(summary = "Admin lấy toàn bộ danh sách dịch vụ và combo")
    public ResponseEntity<List<WashServiceResponse>> getAllServices() {
        List<WashServiceResponse> responses = washServiceRepository.findAll().stream()
                .map(WashServiceResponse::from)
                .toList();
        return ResponseEntity.ok(responses);
    }

    @PostMapping("/api/admin/services")
    @Operation(summary = "Admin tạo dịch vụ hoặc combo mới")
    public ResponseEntity<WashServiceResponse> createService(@RequestBody WashServiceRequest request) {
        if (request.getName() == null || request.getName().trim().isEmpty()) {
            throw new BusinessException("Tên dịch vụ không được trống");
        }
        if (request.getBasePrice() == null) {
            throw new BusinessException("Giá dịch vụ không được trống");
        }
        if (request.getEstimatedDuration() == null) {
            throw new BusinessException("Thời lượng dự kiến không được trống");
        }

        int points = request.getPoints() != null ? request.getPoints() : 0;

        WashService service = WashService.builder()
                .name(request.getName())
                .description(request.getDescription())
                .basePrice(request.getBasePrice())
                .estimatedDuration(request.getEstimatedDuration())
                .isActive(request.isActive())
                .points(points)
                .isCombo(request.isCombo())
                .bundledServiceIds(request.getBundledServiceIds())
                .build();

        WashService saved = washServiceRepository.save(service);
        return ResponseEntity.ok(WashServiceResponse.from(saved));
    }

    @PutMapping("/api/admin/services/{id}")
    @Operation(summary = "Admin cập nhật thông tin dịch vụ hoặc combo")
    public ResponseEntity<WashServiceResponse> updateService(
            @PathVariable UUID id,
            @RequestBody WashServiceRequest request) {
        WashService service = washServiceRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Không tìm thấy dịch vụ hoặc combo"));

        if (request.getName() != null)
            service.setName(request.getName());
        if (request.getDescription() != null)
            service.setDescription(request.getDescription());
        if (request.getBasePrice() != null)
            service.setBasePrice(request.getBasePrice());
        if (request.getEstimatedDuration() != null)
            service.setEstimatedDuration(request.getEstimatedDuration());
        service.setActive(request.isActive());
        if (request.getPoints() != null)
            service.setPoints(request.getPoints());
        service.setCombo(request.isCombo());
        service.setBundledServiceIds(request.getBundledServiceIds());

        WashService updated = washServiceRepository.save(service);
        return ResponseEntity.ok(WashServiceResponse.from(updated));
    }

    @DeleteMapping("/api/admin/services/{id}")
    @Operation(summary = "Admin xóa hoặc hủy kích hoạt dịch vụ")
    public ResponseEntity<Void> deleteService(@PathVariable UUID id) {
        WashService service = washServiceRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Không tìm thấy dịch vụ hoặc combo"));

        // Hủy kích hoạt thay vì xóa cứng để tránh lỗi khóa ngoại với các đặt lịch cũ
        service.setActive(false);
        washServiceRepository.save(service);
        return ResponseEntity.ok().build();
    }
}
