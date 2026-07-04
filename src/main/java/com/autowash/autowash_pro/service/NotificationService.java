package com.autowash.autowash_pro.service;

import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import com.autowash.autowash_pro.dto.response.notification.BookingStatusEvent;
import com.autowash.autowash_pro.dto.response.notification.NotificationEvent;
import com.autowash.autowash_pro.entity.Booking;
import com.autowash.autowash_pro.entity.Customer;
import com.autowash.autowash_pro.enums.NotificationType;
import com.autowash.autowash_pro.enums.Tier;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final SimpMessagingTemplate messagingTemplate;

    public void sendBookingStatusChanged(Booking booking) {
        String message = "Lịch rửa xe của bạn đã chuyển sang trạng thái "
                + booking.getStatus();

        BookingStatusEvent statusEvent = BookingStatusEvent.builder()
                .bookingId(booking.getBookingId())
                .status(booking.getStatus())
                .message(message)
                .updatedAt(LocalDateTime.now())
                .build();

        messagingTemplate.convertAndSend(
                "/topic/booking/" + booking.getBookingId(),
                statusEvent);

        sendToCustomer(
                booking.getCustomer().getCustomerId(),
                NotificationEvent.builder()
                        .type(NotificationType.BOOKING_UPDATE)
                        .title("Cập nhật lịch rửa xe")
                        .message(message)
                        .timestamp(LocalDateTime.now())
                        .build());
    }

    public void sendPointsEarned(
            Customer customer,
            int pointsEarned,
            int newBalance) {

        sendToCustomer(
                customer.getCustomerId(),
                NotificationEvent.builder()
                        .type(NotificationType.POINTS_EARNED)
                        .title("Bạn vừa được tích điểm")
                        .message(String.format(
                                "Bạn nhận được %d điểm. Số dư hiện tại: %d điểm.",
                                pointsEarned,
                                newBalance))
                        .timestamp(LocalDateTime.now())
                        .build());
    }

    public void sendTierChanged(
            Customer customer,
            Tier oldTier,
            Tier newTier) {

        sendToCustomer(
                customer.getCustomerId(),
                NotificationEvent.builder()
                        .type(NotificationType.TIER_UPGRADE)
                        .title("Hạng thành viên đã thay đổi")
                        .message(String.format(
                                "Hạng của bạn đã thay đổi từ %s sang %s.",
                                oldTier,
                                newTier))
                        .timestamp(LocalDateTime.now())
                        .build());
    }

    public void sendPromotionNew(UUID customerId, String title, String message) {
        sendToCustomer(
                customerId,
                NotificationEvent.builder()
                        .type(NotificationType.PROMO_NEW)
                        .title(title)
                        .message(message)
                        .timestamp(LocalDateTime.now())
                        .build());
    }

    public void sendToCustomer(UUID customerId, NotificationEvent event) {
        messagingTemplate.convertAndSend(
                "/topic/customer/" + customerId + "/notifications",
                event);
    }
}
