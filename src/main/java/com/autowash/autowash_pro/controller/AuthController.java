package com.autowash.autowash_pro.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.autowash.autowash_pro.dto.response.auth.AuthResponse;
import com.autowash.autowash_pro.dto.request.auth.LoginRequest;
import com.autowash.autowash_pro.dto.request.auth.RefreshTokenRequest;
import com.autowash.autowash_pro.dto.request.auth.RegisterRequest;
import com.autowash.autowash_pro.dto.request.customer.CustomerRequestDTO;
import com.autowash.autowash_pro.dto.response.customer.CustomerProfileResponse;
import com.autowash.autowash_pro.service.AuthService;
import com.autowash.autowash_pro.service.CustomerService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Auth", description = "Đăng ký, đăng nhập, refresh token")
public class AuthController {

    private final AuthService authService;
    private final CustomerService customerService;

    @PostMapping("/register")
    @Operation(summary = "Đăng ký tài khoản mới")
    public ResponseEntity<AuthResponse> register(
            @RequestBody @Valid RegisterRequest request) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(authService.register(request));
    }

    @PostMapping("/login")
    @Operation(summary = "Đăng nhập bằng email hoặc phone + password")
    public ResponseEntity<AuthResponse> login(
            @RequestBody @Valid LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/refresh")
    @Operation(summary = "Lấy access token mới bằng refresh token")
    public ResponseEntity<AuthResponse> refresh(
            @RequestBody @Valid RefreshTokenRequest request) {
        return ResponseEntity.ok(authService.refreshToken(request));
    }

    @GetMapping("/me")
    @Operation(summary = "Lấy thông tin tài khoản của người dùng hiện tại")
    public ResponseEntity<CustomerProfileResponse> getMyProfile(
            @AuthenticationPrincipal UserDetails userDetails) {
        String phone = userDetails.getUsername();
        return ResponseEntity.ok(customerService.getMyProfile(phone));
    }

    @PutMapping("/me")
    @Operation(summary = "Cập nhật thông tin tài khoản của người dùng hiện tại")
    public ResponseEntity<CustomerProfileResponse> updateMyProfile(
            @RequestBody @Valid CustomerRequestDTO request,
            @AuthenticationPrincipal UserDetails userDetails) {
        String phone = userDetails.getUsername();
        return ResponseEntity.ok(customerService.updateMyProfile(phone, request));
    }
}
