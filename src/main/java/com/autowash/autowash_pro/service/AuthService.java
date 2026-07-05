package com.autowash.autowash_pro.service;

import java.util.Optional;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.autowash.autowash_pro.config.JwtUtil;
import com.autowash.autowash_pro.dto.response.auth.AuthResponse;
import com.autowash.autowash_pro.dto.request.auth.LoginRequest;
import com.autowash.autowash_pro.dto.request.auth.RefreshTokenRequest;
import com.autowash.autowash_pro.dto.request.auth.RegisterRequest;

import com.autowash.autowash_pro.entity.Customer;
import com.autowash.autowash_pro.exception.BusinessException;
import com.autowash.autowash_pro.exception.ResourceNotFoundException;
import com.autowash.autowash_pro.repository.CustomerRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final CustomerRepository customerRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    @Transactional
    public AuthResponse register(RegisterRequest request) {

        // Kiểm tra phone đã tồn tại chưa
        if (customerRepository.existsByPhone(request.getPhone())) {
            throw new BusinessException(
                    "Số điện thoại đã được đăng ký");
        }

        // Kiểm tra email nếu có
        if (request.getEmail() != null
                && customerRepository.existsByEmail(request.getEmail())) {
            throw new BusinessException("Email đã được sử dụng");
        }

        // Tạo customer mới
        Customer customer = Customer.builder()
                .fullName(request.getFullName())
                .phone(request.getPhone())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .build();

        Customer savedCustomer = customerRepository.save(customer);

        // Tạo token ngay sau đăng ký
        return buildAuthResponse(savedCustomer);
    }

    public AuthResponse login(LoginRequest request) {
        String identifier = request.getEmailOrPhone().trim();

        Customer customer = findCustomerByEmailOrPhone(identifier)
                .orElseThrow(() -> new BusinessException("Email hoặc số điện thoại hoặc mật khẩu không đúng"));

        if (!customer.isActive()) {
            throw new BusinessException("Tài khoản đã bị khóa");
        }

        if (!passwordEncoder.matches(
                request.getPassword(), customer.getPassword())) {
            throw new BusinessException(
                    "Email hoặc số điện thoại hoặc mật khẩu không đúng");
        }

        return buildAuthResponse(customer);
    }

    private Optional<Customer> findCustomerByEmailOrPhone(String identifier) {
        if (isEmail(identifier)) {
            return customerRepository.findByEmail(identifier);
        }
        return customerRepository.findByPhone(identifier);
    }

    private boolean isEmail(String value) {
        return value != null && value.contains("@");
    }

    public AuthResponse refreshToken(RefreshTokenRequest request) {

        if (!jwtUtil.isTokenValid(request.getRefreshToken())) {
            throw new BusinessException("Refresh token không hợp lệ");
        }

        String phone = jwtUtil.extractPhone(request.getRefreshToken());
        Customer customer = customerRepository
                .findByPhone(phone)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy user"));

        return buildAuthResponse(customer);
    }

    // Helper dùng chung
    private AuthResponse buildAuthResponse(Customer customer) {
        String role = customer.isAdmin() ? "ADMIN" : "CUSTOMER";
        return AuthResponse.builder()
                .id(customer.getCustomerId())
                .accessToken(jwtUtil.generateAccessToken(
                        customer.getPhone(), role))
                .refreshToken(jwtUtil.generateRefreshToken(
                        customer.getPhone()))
                .tokenType("Bearer")
                .role(role)
                .tier(customer.getTier())
                .fullName(customer.getFullName())
                .phone(customer.getPhone())
                .build();
    }
}
