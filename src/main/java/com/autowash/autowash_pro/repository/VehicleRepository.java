package com.autowash.autowash_pro.repository;

import com.autowash.autowash_pro.entity.Vehicle;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface VehicleRepository
        extends JpaRepository<Vehicle, UUID> {

    List<Vehicle> findByCustomer_CustomerId(UUID customerId);

    Optional<Vehicle> findByLicensePlate(String licensePlate);

    Optional<Vehicle> findByVehicleIdAndCustomer_CustomerId(
            UUID vehicleId, UUID customerId);

    boolean existsByLicensePlate(String licensePlate);

    Optional<Vehicle> findByCustomer_CustomerIdAndIsPrimaryTrue(UUID customerId);

    List<Vehicle> findByCustomer_CustomerIdOrderByCreatedAtDesc(UUID customerId);

    long countByCustomer_CustomerId(UUID customerId);

    boolean existsByLicensePlateAndVehicleIdNot(String licensePlate, UUID vehicleId);
}
