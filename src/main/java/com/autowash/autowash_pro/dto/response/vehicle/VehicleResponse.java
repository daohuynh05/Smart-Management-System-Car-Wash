package com.autowash.autowash_pro.dto.response.vehicle;

import java.time.LocalDateTime;
import java.util.UUID;

import com.autowash.autowash_pro.entity.Vehicle;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class VehicleResponse {

    private UUID vehicleId;
    private String licensePlate;
    private String vehicleType;
    private String brand;
    private String color;
    private boolean primary;
    private String lastWashService;
    private String lastWashDate;
    private LocalDateTime createdAt;

    public static VehicleResponse from(Vehicle vehicle) {
        return VehicleResponse.builder()
            .vehicleId(vehicle.getVehicleId())
            .licensePlate(vehicle.getLicensePlate())
            .vehicleType(vehicle.getVehicleType())
            .brand(vehicle.getBrand())
            .color(vehicle.getColor())
            .primary(vehicle.isPrimary())
            .createdAt(vehicle.getCreatedAt())
            .build();
    }
}
