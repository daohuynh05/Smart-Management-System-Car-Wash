package com.autowash.autowash_pro.controller;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.autowash.autowash_pro.dto.request.booking.CreateBookingRequest;
import com.autowash.autowash_pro.dto.request.booking.UpdateBookingStatusRequest;
import com.autowash.autowash_pro.dto.response.booking.AvailabilitySlotResponse;
import com.autowash.autowash_pro.dto.response.booking.BookingResponse;
import com.autowash.autowash_pro.enums.BookingStatus;
import com.autowash.autowash_pro.service.BookingService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@Tag(name = "Booking", description = "Đặt lịch và quản lý lịch rửa xe")
public class BookingController {

    private final BookingService bookingService;

    @PostMapping("/api/bookings")
    @Operation(summary = "Tạo lịch rửa xe cho khách hàng hiện tại")
    public ResponseEntity<BookingResponse> createBooking(
            @RequestBody @Valid CreateBookingRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(bookingService.createBooking(request, userDetails));
    }

    @GetMapping("/api/bookings")
    @Operation(summary = "Xem danh sách lịch rửa xe của khách hàng hiện tại")
    public ResponseEntity<List<BookingResponse>> getMyBookings(
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(bookingService.getMyBookings(userDetails));
    }

    @GetMapping("/api/bookings/{bookingId}")
    @Operation(summary = "Xem chi tiết một lịch rửa xe")
    public ResponseEntity<BookingResponse> getBooking(
            @PathVariable UUID bookingId,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(
                bookingService.getBooking(bookingId, userDetails));
    }

    @GetMapping("/api/bookings/availability")
    @Operation(summary = "Xem các slot còn trống trong ngày")
    public ResponseEntity<List<AvailabilitySlotResponse>> getAvailability(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(
                bookingService.getAvailability(date, userDetails));
    }

    @PatchMapping("/api/bookings/{bookingId}/cancel")
    @Operation(summary = "Hủy lịch rửa xe")
    public ResponseEntity<BookingResponse> cancelBooking(
            @PathVariable UUID bookingId,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(
                bookingService.cancelBooking(bookingId, userDetails));
    }

    @GetMapping("/api/admin/bookings")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Admin xem danh sách booking theo ngày hoặc trạng thái")
    public ResponseEntity<List<BookingResponse>> getAdminBookings(
            @RequestParam(required = false) BookingStatus status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(
                bookingService.getAdminBookings(status, date));
    }

    @PatchMapping("/api/admin/bookings/{bookingId}/status")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Admin cập nhật trạng thái booking")
    public ResponseEntity<BookingResponse> updateStatus(
            @PathVariable UUID bookingId,
            @RequestBody @Valid UpdateBookingStatusRequest request) {
        return ResponseEntity.ok(
                bookingService.updateStatus(bookingId, request.getStatus(), request.getPromoId()));
    }
}