package com.autowash.autowash_pro.config;

import java.math.BigDecimal;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import com.autowash.autowash_pro.entity.Customer;
import com.autowash.autowash_pro.entity.Vehicle;
import com.autowash.autowash_pro.entity.WashService;
import com.autowash.autowash_pro.repository.CustomerRepository;
import com.autowash.autowash_pro.repository.VehicleRepository;
import com.autowash.autowash_pro.repository.WashServiceRepository;
import com.autowash.autowash_pro.service.AdminConfigService;
import lombok.RequiredArgsConstructor;

/** Synthetic fixtures available only in the explicitly activated demo profile. */
@Component
@Profile("demo")
@RequiredArgsConstructor
public class DemoDataInitializer implements CommandLineRunner {
    private final CustomerRepository customers;
    private final VehicleRepository vehicles;
    private final WashServiceRepository services;
    private final PasswordEncoder encoder;
    private final AdminConfigService configService;

    @Value("${demo.customer-password}")
    private String customerPassword;
    @Value("${demo.admin-password}")
    private String adminPassword;

    @Override
    @Transactional
    public void run(String... args) {
        Customer customer = createAccount("0000000001", "customer@example.invalid",
                "Demo Customer", customerPassword, false);
        createAccount("0000000002", "admin@example.invalid", "Demo Admin", adminPassword, true);
        if (!vehicles.existsByLicensePlate("DEMO-0001")) {
            vehicles.save(Vehicle.builder().customer(customer).licensePlate("DEMO-0001")
                    .vehicleType("MOTORBIKE").brand("Demo").color("Blue").isPrimary(true).build());
        }
        if (services.count() == 0) {
            services.save(WashService.builder().name("Demo Basic Wash").description("Synthetic demo service")
                    .basePrice(new BigDecimal("50000")).estimatedDuration(30).points(5).build());
        }
        configService.getSystemConfig();
    }

    private Customer createAccount(String phone, String email, String name, String password, boolean admin) {
        // Preserve changes to an existing demo account when the app restarts.
        return customers.findByPhone(phone).orElseGet(() -> customers.save(Customer.builder()
                .phone(phone).email(email).fullName(name).password(encoder.encode(password))
                .isAdmin(admin).isActive(true).build()));
    }
}
