package com.autowash.autowash_pro.controller;

import com.autowash.autowash_pro.dto.request.customer.CustomerRequestDTO;
import com.autowash.autowash_pro.dto.response.customer.CustomerProfileResponse;
import com.autowash.autowash_pro.dto.response.vehicle.VehicleResponse;
import com.autowash.autowash_pro.dto.response.booking.BookingResponse;
import com.autowash.autowash_pro.entity.Customer;
import com.autowash.autowash_pro.service.CustomerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/customers")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Customer Admin", description = "Quản lý danh sách khách hàng dành cho Admin")
public class CustomerController {

    private final CustomerService customerService;

    @GetMapping
    @Operation(summary = "Lấy danh sách khách hàng với bộ lọc tìm kiếm và hạng thành viên")
    public ResponseEntity<List<CustomerProfileResponse>> getAdminCustomers(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String tier) {
        List<CustomerProfileResponse> responses = customerService.getAdminCustomers(keyword, tier).stream()
                .map(CustomerProfileResponse::from)
                .toList();
        return ResponseEntity.ok(responses);
    }

    @PostMapping
    @Operation(summary = "Tạo mới khách hàng tại quầy")
    public ResponseEntity<CustomerProfileResponse> addCustomer(@RequestBody CustomerRequestDTO request) {
        Customer customer = customerService.createCustomerAtCounter(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(CustomerProfileResponse.from(customer));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Lấy chi tiết khách hàng theo ID")
    public ResponseEntity<CustomerProfileResponse> getCustomerById(@PathVariable UUID id) {
        Customer customer = customerService.getCustomerById(id);
        return ResponseEntity.ok(CustomerProfileResponse.from(customer));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Cập nhật thông tin khách hàng")
    public ResponseEntity<CustomerProfileResponse> updateCustomer(@PathVariable UUID id,
            @RequestBody CustomerRequestDTO dto) {
        Customer customer = customerService.updateCustomer(id, dto);
        return ResponseEntity.ok(CustomerProfileResponse.from(customer));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Vô hiệu hóa khách hàng")
    public ResponseEntity<String> disableCustomer(@PathVariable UUID id) {
        customerService.disableCustomer(id);
        return ResponseEntity.ok("Đã vô hiệu hóa khách hàng thành công!");
    }

    @GetMapping("/{id}/vehicles")
    @Operation(summary = "Lấy danh sách xe của khách hàng")
    public ResponseEntity<List<VehicleResponse>> getVehiclesByCustomerId(@PathVariable UUID id) {
        List<VehicleResponse> responses = customerService.getVehiclesByCustomerId(id).stream()
                .map(VehicleResponse::from)
                .toList();
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/{id}/history")
    @Operation(summary = "Lấy lịch sử đặt lịch của khách hàng")
    public ResponseEntity<List<BookingResponse>> getBookingHistoryByCustomerId(@PathVariable UUID id) {
        List<BookingResponse> responses = customerService.getBookingHistoryByCustomerId(id).stream()
                .map(BookingResponse::from)
                .toList();
        return ResponseEntity.ok(responses);
    }
}