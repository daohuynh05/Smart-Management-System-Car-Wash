package com.autowash.autowash_pro.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.autowash.autowash_pro.service.CustomerService;

@ExtendWith(MockitoExtension.class)
public class CustomerControllerTest {

    private MockMvc mockMvc;

    @Mock
    private CustomerService customerService;

    @InjectMocks
    private CustomerController customerController;

    @BeforeEach
    public void setup() {
        mockMvc = MockMvcBuilders.standaloneSetup(customerController).build();
    }

    // Test Case 1: GET All Customers
    @Test
    public void testGetAllCustomers_Success() throws Exception {
        when(customerService.getAllCustomers()).thenReturn(new ArrayList<>());
        mockMvc.perform(get("/api/admin/customers")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    // Test Case 2: GET Customer Vehicles
    @Test
    public void testGetCustomerVehicles_Success() throws Exception {
        UUID customerId = UUID.randomUUID();
        when(customerService.getVehiclesByCustomerId(customerId)).thenReturn(new ArrayList<>());
        mockMvc.perform(get("/api/admin/customers/" + customerId + "/vehicles")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    // Test Case 3: GET Customer Wash History
    @Test
    public void testGetCustomerHistory_Success() throws Exception {
        UUID customerId = UUID.randomUUID();
        when(customerService.getBookingHistoryByCustomerId(customerId)).thenReturn(new ArrayList<>());
        mockMvc.perform(get("/api/admin/customers/" + customerId + "/history")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    // Test Case 4: POST Create New Customer
    @Test
    public void testCreateCustomer_Success() throws Exception {
        String customerJson = "{\"name\":\"Nguyen Van A\",\"phone\":\"0987654321\",\"email\":\"nva@gmail.com\"}";
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/admin/customers")
                .contentType(MediaType.APPLICATION_JSON)
                .content(customerJson))
                .andExpect(status().isOk());
    }

    // Test Case 5: PUT Update Existing Customer
    @Test
    public void testUpdateCustomer_Success() throws Exception {
        UUID customerId = UUID.randomUUID();
        String updatedCustomerJson = "{\"name\":\"Nguyen Van A Updated\",\"phone\":\"0987654321\",\"email\":\"nva_updated@gmail.com\"}";
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put("/api/admin/customers/" + customerId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(updatedCustomerJson))
                .andExpect(status().isOk());
    }

    // Test Case 6: DELETE Disable Customer Account
    @Test
    public void testDisableCustomer_Success() throws Exception {
        UUID customerId = UUID.randomUUID();
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete("/api/admin/customers/" + customerId)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }
}