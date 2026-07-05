package com.autowash.autowash_pro.controller;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.autowash.autowash_pro.dto.request.auth.ChangePasswordRequest;
import com.autowash.autowash_pro.exception.BusinessException;
import com.autowash.autowash_pro.exception.GlobalExceptionHandler;
import com.autowash.autowash_pro.service.CustomerService;

@ExtendWith(MockitoExtension.class)
public class CustomerProfileControllerTest {

    private MockMvc mockMvc;

    @Mock
    private CustomerService customerService;

    @InjectMocks
    private CustomerProfileController customerProfileController;

    @BeforeEach
    public void setup() {
        mockMvc = MockMvcBuilders.standaloneSetup(customerProfileController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    public void testChangePassword_Success() throws Exception {
        UUID customerId = UUID.randomUUID();
        
        doNothing().when(customerService).changePassword(eq(customerId), any(ChangePasswordRequest.class));

        String requestJson = "{\"oldPassword\":\"oldPassword123\",\"newPassword\":\"newPassword123\"}";

        mockMvc.perform(patch("/api/customers/" + customerId + "/password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Đổi mật khẩu thành công!"));
    }

    @Test
    public void testChangePassword_ValidationFailed_EmptyOldPassword() throws Exception {
        UUID customerId = UUID.randomUUID();

        String requestJson = "{\"oldPassword\":\"\",\"newPassword\":\"newPassword123\"}";

        mockMvc.perform(patch("/api/customers/" + customerId + "/password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Mật khẩu cũ không được để trống")));
    }

    @Test
    public void testChangePassword_ValidationFailed_NewPasswordTooShort() throws Exception {
        UUID customerId = UUID.randomUUID();

        String requestJson = "{\"oldPassword\":\"oldPassword123\",\"newPassword\":\"12345\"}";

        mockMvc.perform(patch("/api/customers/" + customerId + "/password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Mật khẩu mới ít nhất 6 ký tự")));
    }

    @Test
    public void testChangePassword_BusinessError_WrongOldPassword() throws Exception {
        UUID customerId = UUID.randomUUID();

        doThrow(new BusinessException("Mật khẩu cũ không đúng"))
                .when(customerService).changePassword(eq(customerId), any(ChangePasswordRequest.class));

        String requestJson = "{\"oldPassword\":\"wrongOldPassword\",\"newPassword\":\"newPassword123\"}";

        mockMvc.perform(patch("/api/customers/" + customerId + "/password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BUSINESS_ERROR"))
                .andExpect(jsonPath("$.message").value("Mật khẩu cũ không đúng"));
    }
}
