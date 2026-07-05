package com.autowash.autowash_pro.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.autowash.autowash_pro.dto.request.auth.ChangePasswordRequest;
import com.autowash.autowash_pro.entity.Customer;
import com.autowash.autowash_pro.entity.Vehicle;
import com.autowash.autowash_pro.exception.BusinessException;
import com.autowash.autowash_pro.repository.CustomerRepository;
import com.autowash.autowash_pro.repository.VehicleRepository;

import com.autowash.autowash_pro.entity.Booking; 
import com.autowash.autowash_pro.repository.BookingRepository; 

@ExtendWith(MockitoExtension.class)
public class CustomerServiceTest {

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private VehicleRepository vehicleRepository;

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private CustomerService customerService;

    // Test Case 1: Kiểm thử độc lập logic lấy danh sách xe của Service
    @Test
    public void testGetVehiclesByCustomerId_ShouldReturnList() {
        UUID customerId = UUID.randomUUID();
        when(vehicleRepository.findByCustomer_CustomerId(customerId)).thenReturn(new ArrayList<>());

        List<Vehicle> vehicles = customerService.getVehiclesByCustomerId(customerId);
        
        assertNotNull(vehicles);
    }

    // Test Case 2: Kiểm thử độc lập logic lấy lịch sử rửa xe của Service
    @Test
    public void testGetBookingHistoryByCustomerId_ShouldReturnList() {
        UUID customerId = UUID.randomUUID();
        when(bookingRepository.findByCustomer_CustomerId(customerId)).thenReturn(new ArrayList<>());

        List<Booking> history = customerService.getBookingHistoryByCustomerId(customerId);
        assertNotNull(history);
    }

    @Test
    public void testChangePassword_Success() {
        UUID customerId = UUID.randomUUID();
        Customer customer = new Customer();
        customer.setPassword("hashedOldPassword");

        when(customerRepository.findById(customerId)).thenReturn(Optional.of(customer));
        when(passwordEncoder.matches("oldPassword", "hashedOldPassword")).thenReturn(true);
        when(passwordEncoder.encode("newPassword")).thenReturn("hashedNewPassword");
        when(customerRepository.save(any(Customer.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ChangePasswordRequest request = new ChangePasswordRequest("oldPassword", "newPassword");
        customerService.changePassword(customerId, request);

        assertEquals("hashedNewPassword", customer.getPassword());
        verify(customerRepository, times(1)).save(customer);
    }

    @Test
    public void testChangePassword_WrongOldPassword_ThrowsException() {
        UUID customerId = UUID.randomUUID();
        Customer customer = new Customer();
        customer.setPassword("hashedOldPassword");

        when(customerRepository.findById(customerId)).thenReturn(Optional.of(customer));
        when(passwordEncoder.matches("wrongOldPassword", "hashedOldPassword")).thenReturn(false);

        ChangePasswordRequest request = new ChangePasswordRequest("wrongOldPassword", "newPassword");
        
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            customerService.changePassword(customerId, request);
        });

        assertEquals("Mật khẩu cũ không đúng", exception.getMessage());
        verify(customerRepository, never()).save(any(Customer.class));
    }

    @Test
    public void testChangePassword_NewPasswordTooShort_ThrowsException() {
        UUID customerId = UUID.randomUUID();
        Customer customer = new Customer();
        customer.setPassword("hashedOldPassword");

        when(customerRepository.findById(customerId)).thenReturn(Optional.of(customer));
        when(passwordEncoder.matches("oldPassword", "hashedOldPassword")).thenReturn(true);

        ChangePasswordRequest request = new ChangePasswordRequest("oldPassword", "12345");
        
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            customerService.changePassword(customerId, request);
        });

        assertEquals("Mật khẩu mới ít nhất 6 ký tự", exception.getMessage());
        verify(customerRepository, never()).save(any(Customer.class));
    }
}