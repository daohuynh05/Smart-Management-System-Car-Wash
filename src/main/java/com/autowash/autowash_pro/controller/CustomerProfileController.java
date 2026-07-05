package com.autowash.autowash_pro.controller;

import com.autowash.autowash_pro.dto.request.auth.ChangePasswordRequest;
import com.autowash.autowash_pro.dto.request.auth.MessageResponse;
import com.autowash.autowash_pro.service.CustomerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/customers")
@CrossOrigin(origins = "*")
@Tag(name = "Customer Profile", description = "Các API dành cho khách hàng tự quản lý tài khoản")
public class CustomerProfileController {

    @Autowired
    private CustomerService customerService;

    @PatchMapping("/{id}/password")
    @Operation(summary = "Đổi mật khẩu cho khách hàng")
    public ResponseEntity<MessageResponse> changePassword(
            @PathVariable UUID id,
            @RequestBody @Valid ChangePasswordRequest request) {
        customerService.changePassword(id, request);
        return ResponseEntity.ok(new MessageResponse("Đổi mật khẩu thành công!"));
    }
}
