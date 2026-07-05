package com.autowash.autowash_pro.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.autowash.autowash_pro.config.JwtUtil;
import com.autowash.autowash_pro.dto.response.auth.AuthResponse;
import com.autowash.autowash_pro.dto.request.auth.LoginRequest;
import com.autowash.autowash_pro.dto.request.auth.RefreshTokenRequest;
import com.autowash.autowash_pro.dto.request.auth.RegisterRequest;
import com.autowash.autowash_pro.entity.Customer;
import com.autowash.autowash_pro.enums.Tier;
import com.autowash.autowash_pro.exception.BusinessException;
import com.autowash.autowash_pro.repository.CustomerRepository;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtUtil jwtUtil;

    @InjectMocks
    private AuthService authService;

    private Customer customer;

    @BeforeEach
    void setUp() {
        customer = Customer.builder()
                .customerId(UUID.randomUUID())
                .fullName("Nguyen Van A")
                .phone("0987654321")
                .email("a@gmail.com")
                .password("encodedPassword")
                .isActive(true)
                .isAdmin(false)
                .tier(Tier.MEMBER)
                .build();
    }

    @Test
    void register_ShouldSaveCustomerAndReturnAuthResponse_WhenRequestIsValid() {
        RegisterRequest request = new RegisterRequest("Nguyen Van A", "0987654321", "a@gmail.com", "password123");

        when(customerRepository.existsByPhone(request.getPhone())).thenReturn(false);
        when(customerRepository.existsByEmail(request.getEmail())).thenReturn(false);
        when(passwordEncoder.encode(request.getPassword())).thenReturn("encodedPassword");
        when(customerRepository.save(any(Customer.class))).thenReturn(customer);
        when(jwtUtil.generateAccessToken(any(), any())).thenReturn("mockedAccessToken");
        when(jwtUtil.generateRefreshToken(any())).thenReturn("mockedRefreshToken");

        AuthResponse response = authService.register(request);

        assertNotNull(response);
        assertEquals(customer.getCustomerId(), response.getId());
        assertEquals("mockedAccessToken", response.getAccessToken());
        assertEquals("mockedRefreshToken", response.getRefreshToken());
        assertEquals("Nguyen Van A", response.getFullName());
        verify(customerRepository, times(1)).save(any(Customer.class));
    }

    @Test
    void register_ShouldThrowException_WhenPhoneAlreadyExists() {
        RegisterRequest request = new RegisterRequest("Nguyen Van A", "0987654321", "a@gmail.com", "password123");

        when(customerRepository.existsByPhone(request.getPhone())).thenReturn(true);

        BusinessException exception = assertThrows(BusinessException.class, () -> authService.register(request));
        assertEquals("Số điện thoại đã được đăng ký", exception.getMessage());
        verify(customerRepository, never()).save(any(Customer.class));
    }

    @Test
    void login_ShouldReturnAuthResponse_WhenCredentialsAreValid() {
        LoginRequest request = new LoginRequest("0987654321", "password123");

        when(customerRepository.findByPhone(request.getEmailOrPhone())).thenReturn(Optional.of(customer));
        when(passwordEncoder.matches(request.getPassword(), customer.getPassword())).thenReturn(true);
        when(jwtUtil.generateAccessToken(any(), any())).thenReturn("mockedAccessToken");
        when(jwtUtil.generateRefreshToken(any())).thenReturn("mockedRefreshToken");

        AuthResponse response = authService.login(request);

        assertNotNull(response);
        assertEquals(customer.getCustomerId(), response.getId());
        assertEquals("mockedAccessToken", response.getAccessToken());
    }

    @Test
    void login_ShouldReturnAuthResponse_WhenLoggingInWithEmail() {
        LoginRequest request = new LoginRequest("a@gmail.com", "password123");

        when(customerRepository.findByEmail(request.getEmailOrPhone())).thenReturn(Optional.of(customer));
        when(passwordEncoder.matches(request.getPassword(), customer.getPassword())).thenReturn(true);
        when(jwtUtil.generateAccessToken(any(), any())).thenReturn("mockedAccessToken");
        when(jwtUtil.generateRefreshToken(any())).thenReturn("mockedRefreshToken");

        AuthResponse response = authService.login(request);

        assertNotNull(response);
        assertEquals(customer.getCustomerId(), response.getId());
        assertEquals("mockedAccessToken", response.getAccessToken());
    }

    @Test
    void login_ShouldThrowException_WhenUserIsInactive() {
        LoginRequest request = new LoginRequest("0987654321", "password123");
        customer.setActive(false);

        when(customerRepository.findByPhone(request.getEmailOrPhone())).thenReturn(Optional.of(customer));

        BusinessException exception = assertThrows(BusinessException.class, () -> authService.login(request));
        assertEquals("Tài khoản đã bị khóa", exception.getMessage());
    }

    @Test
    void login_ShouldThrowException_WhenPasswordIsIncorrect() {
        LoginRequest request = new LoginRequest("0987654321", "wrongPassword");

        when(customerRepository.findByPhone(request.getEmailOrPhone())).thenReturn(Optional.of(customer));
        when(passwordEncoder.matches(request.getPassword(), customer.getPassword())).thenReturn(false);

        BusinessException exception = assertThrows(BusinessException.class, () -> authService.login(request));
        assertEquals("Email hoặc số điện thoại hoặc mật khẩu không đúng", exception.getMessage());
    }

    @Test
    void refreshToken_ShouldReturnNewTokens_WhenRefreshTokenIsValid() {
        RefreshTokenRequest request = new RefreshTokenRequest("validRefreshToken");

        when(jwtUtil.isTokenValid("validRefreshToken")).thenReturn(true);
        when(jwtUtil.extractPhone("validRefreshToken")).thenReturn("0987654321");
        when(customerRepository.findByPhone("0987654321")).thenReturn(Optional.of(customer));
        when(jwtUtil.generateAccessToken(any(), any())).thenReturn("newAccessToken");
        when(jwtUtil.generateRefreshToken(any())).thenReturn("newRefreshToken");

        AuthResponse response = authService.refreshToken(request);

        assertNotNull(response);
        assertEquals(customer.getCustomerId(), response.getId());
        assertEquals("newAccessToken", response.getAccessToken());
        assertEquals("newRefreshToken", response.getRefreshToken());
    }

    @Test
    void refreshToken_ShouldThrowException_WhenTokenIsInvalid() {
        RefreshTokenRequest request = new RefreshTokenRequest("invalidRefreshToken");

        when(jwtUtil.isTokenValid("invalidRefreshToken")).thenReturn(false);

        BusinessException exception = assertThrows(BusinessException.class, () -> authService.refreshToken(request));
        assertEquals("Refresh token không hợp lệ", exception.getMessage());
    }
}
