package com.autowash.autowash_pro.service;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.autowash.autowash_pro.dto.request.vehicle.VehicleRequest;
import com.autowash.autowash_pro.dto.response.vehicle.VehicleResponse;
import com.autowash.autowash_pro.entity.Booking;
import com.autowash.autowash_pro.enums.BookingStatus;
import com.autowash.autowash_pro.entity.Customer;
import com.autowash.autowash_pro.entity.Vehicle;
import com.autowash.autowash_pro.exception.BusinessException;
import com.autowash.autowash_pro.exception.ResourceNotFoundException;
import com.autowash.autowash_pro.repository.BookingRepository;
import com.autowash.autowash_pro.repository.CustomerRepository;
import com.autowash.autowash_pro.repository.VehicleRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class VehicleService {

    private final VehicleRepository vehicleRepository;
    private final CustomerRepository customerRepository;
    private final BookingRepository bookingRepository;

    private VehicleResponse mapToResponse(Vehicle vehicle) {
        List<Booking> bookings = bookingRepository.findByVehicle_VehicleIdAndStatusOrderByScheduledAtDesc(
                vehicle.getVehicleId(), BookingStatus.DONE);

        String lastWashService = null;
        String lastWashDate = null;

        if (!bookings.isEmpty()) {
            Booking lastBooking = bookings.get(0);
            if (lastBooking.getBookingServices() != null && !lastBooking.getBookingServices().isEmpty()) {
                lastWashService = lastBooking.getBookingServices().stream()
                        .map(bs -> bs.getWashService().getName())
                        .collect(Collectors.joining(", "));
            } else if (lastBooking.getServiceType() != null) {
                lastWashService = lastBooking.getServiceType().name();
            }

            if (lastBooking.getScheduledAt() != null) {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
                lastWashDate = lastBooking.getScheduledAt().format(formatter);
            }
        }

        return VehicleResponse.builder()
                .vehicleId(vehicle.getVehicleId())
                .licensePlate(vehicle.getLicensePlate())
                .vehicleType(vehicle.getVehicleType())
                .brand(vehicle.getBrand())
                .color(vehicle.getColor())
                .primary(vehicle.isPrimary())
                .createdAt(vehicle.getCreatedAt())
                .lastWashService(lastWashService)
                .lastWashDate(lastWashDate)
                .build();
    }

    @Transactional(readOnly = true)
    public List<VehicleResponse> getMyVehicles(String phone) {
        Customer customer = getCustomer(phone);
        return vehicleRepository
                .findByCustomer_CustomerIdOrderByCreatedAtDesc(
                        customer.getCustomerId())
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Transactional
    public VehicleResponse createVehicle(String phone, VehicleRequest request) {
        Customer customer = getCustomer(phone);
        String licensePlate = normalizeLicensePlate(
                request.getLicensePlate());

        if (vehicleRepository.existsByLicensePlate(licensePlate)) {
            throw new BusinessException("Bien so xe da ton tai");
        }

        boolean hasNoVehicles = vehicleRepository
                .countByCustomer_CustomerId(customer.getCustomerId()) == 0;
        boolean shouldBePrimary = hasNoVehicles
                || Boolean.TRUE.equals(request.getPrimary());

        if (shouldBePrimary) {
            clearPrimaryVehicle(customer.getCustomerId());
        }

        Vehicle vehicle = Vehicle.builder()
                .customer(customer)
                .licensePlate(licensePlate)
                .vehicleType(trim(request.getVehicleType()))
                .brand(trim(request.getBrand()))
                .color(trim(request.getColor()))
                .isPrimary(shouldBePrimary)
                .build();

        return mapToResponse(vehicleRepository.save(vehicle));
    }

    @Transactional
    public VehicleResponse updateVehicle(
            String phone, UUID vehicleId, VehicleRequest request) {
        Customer customer = getCustomer(phone);
        Vehicle vehicle = getOwnedVehicle(vehicleId, customer);
        String licensePlate = normalizeLicensePlate(
                request.getLicensePlate());

        if (vehicleRepository.existsByLicensePlateAndVehicleIdNot(
                licensePlate, vehicleId)) {
            throw new BusinessException("Bien so xe da ton tai");
        }

        boolean shouldBePrimary = Boolean.TRUE.equals(request.getPrimary());
        if (shouldBePrimary) {
            clearPrimaryVehicle(customer.getCustomerId());
        }

        vehicle.setLicensePlate(licensePlate);
        vehicle.setVehicleType(trim(request.getVehicleType()));
        vehicle.setBrand(trim(request.getBrand()));
        vehicle.setColor(trim(request.getColor()));
        vehicle.setPrimary(shouldBePrimary || vehicle.isPrimary());

        return mapToResponse(vehicleRepository.save(vehicle));
    }

    @Transactional
    public void deleteVehicle(String phone, UUID vehicleId) {
        Customer customer = getCustomer(phone);
        Vehicle vehicle = getOwnedVehicle(vehicleId, customer);

        if (bookingRepository.existsByVehicle_VehicleId(vehicleId)) {
            throw new BusinessException(
                    "Khong the xoa xe da co lich dat");
        }

        boolean wasPrimary = vehicle.isPrimary();
        vehicleRepository.delete(vehicle);

        if (wasPrimary) {
            vehicleRepository
                    .findByCustomer_CustomerIdOrderByCreatedAtDesc(
                            customer.getCustomerId())
                    .stream()
                    .findFirst()
                    .ifPresent(nextPrimary -> {
                        nextPrimary.setPrimary(true);
                        vehicleRepository.save(nextPrimary);
                    });
        }
    }

    @Transactional
    public VehicleResponse setPrimaryVehicle(String phone, UUID vehicleId) {
        Customer customer = getCustomer(phone);
        Vehicle vehicle = getOwnedVehicle(vehicleId, customer);

        clearPrimaryVehicle(customer.getCustomerId());
        vehicle.setPrimary(true);

        return mapToResponse(vehicleRepository.save(vehicle));
    }

    private Customer getCustomer(String phone) {
        return customerRepository.findByPhone(phone)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Khong tim thay khach hang"));
    }

    private Vehicle getOwnedVehicle(UUID vehicleId, Customer customer) {
        return vehicleRepository
                .findByVehicleIdAndCustomer_CustomerId(
                        vehicleId, customer.getCustomerId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Khong tim thay xe"));
    }

    private void clearPrimaryVehicle(UUID customerId) {
        vehicleRepository
                .findByCustomer_CustomerIdAndIsPrimaryTrue(customerId)
                .ifPresent(primaryVehicle -> {
                    primaryVehicle.setPrimary(false);
                    vehicleRepository.save(primaryVehicle);
                });
    }

    private String normalizeLicensePlate(String licensePlate) {
        return trim(licensePlate).toUpperCase();
    }

    private String trim(String value) {
        return value == null ? null : value.trim();
    }
}
