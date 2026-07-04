package com.autowash.autowash_pro.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.autowash.autowash_pro.dto.request.booking.CreateBookingRequest;
import com.autowash.autowash_pro.dto.response.booking.AvailabilitySlotResponse;
import com.autowash.autowash_pro.dto.response.booking.BookingResponse;
import com.autowash.autowash_pro.entity.Booking;
import com.autowash.autowash_pro.entity.Customer;
import com.autowash.autowash_pro.entity.Vehicle;
import com.autowash.autowash_pro.entity.WashService;
import com.autowash.autowash_pro.entity.BookingWashService;
import com.autowash.autowash_pro.enums.BookingStatus;
import com.autowash.autowash_pro.enums.Tier;
import com.autowash.autowash_pro.enums.ServiceType;
import com.autowash.autowash_pro.exception.BusinessException;
import com.autowash.autowash_pro.exception.ResourceNotFoundException;
import com.autowash.autowash_pro.repository.BookingRepository;
import com.autowash.autowash_pro.repository.CustomerRepository;
import com.autowash.autowash_pro.repository.VehicleRepository;
import com.autowash.autowash_pro.repository.WashServiceRepository;
import com.autowash.autowash_pro.repository.BookingWashServiceRepository;
import com.autowash.autowash_pro.repository.PromotionRepository;
import com.autowash.autowash_pro.entity.Promotion;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class BookingService {

    private final WashServiceRepository washServiceRepository;
    private final BookingWashServiceRepository bookingWashServiceRepository;

    @jakarta.annotation.PostConstruct
    public void seedServices() {
        if (washServiceRepository.count() == 0) {
            washServiceRepository.save(WashService.builder()
                    .name("Rửa xe thường")
                    .description("Rửa vỏ xe, lau khô, hút bụi thảm lót chân.")
                    .basePrice(new BigDecimal("30000"))
                    .estimatedDuration(30)
                    .isActive(true)
                    .points(6)
                    .isCombo(false)
                    .build());
            washServiceRepository.save(WashService.builder()
                    .name("Rửa xe cao cấp")
                    .description("Rửa chi tiết vỏ xe, hút bụi toàn bộ nội thất, dưỡng lốp dưỡng nhựa.")
                    .basePrice(new BigDecimal("50000"))
                    .estimatedDuration(45)
                    .isActive(true)
                    .points(10)
                    .isCombo(false)
                    .build());
            washServiceRepository.save(WashService.builder()
                    .name("Chăm sóc toàn diện")
                    .description("Tẩy nhựa đường, tẩy bụi sắt, đánh bóng nhanh vỏ xe, vệ sinh khoang động cơ.")
                    .basePrice(new BigDecimal("80000"))
                    .estimatedDuration(90)
                    .isActive(true)
                    .points(16)
                    .isCombo(false)
                    .build());
            washServiceRepository.save(WashService.builder()
                    .name("Vệ sinh nội thất")
                    .description("Dọn sâu nội thất, giặt ghế nỉ/dưỡng ghế da, khử mùi ozon.")
                    .basePrice(new BigDecimal("120000"))
                    .estimatedDuration(120)
                    .isActive(true)
                    .points(24)
                    .isCombo(false)
                    .build());
            washServiceRepository.save(WashService.builder()
                    .name("Thay nhớt động cơ")
                    .description("Thay nhớt chính hãng Castrol/Mobil1, kiểm tra mực nước làm mát.")
                    .basePrice(new BigDecimal("150000"))
                    .estimatedDuration(20)
                    .isActive(true)
                    .points(30)
                    .isCombo(false)
                    .build());
        }
    }

    private static final LocalTime OPEN_TIME = LocalTime.of(8, 0);
    private static final LocalTime CLOSE_TIME = LocalTime.of(18, 0);
    private static final int SLOT_MINUTES = 30;
    private static final int SLOT_CAPACITY = 2;
    private static final List<BookingStatus> ACTIVE_STATUSES = List.of(
            BookingStatus.PENDING,
            BookingStatus.CONFIRMED,
            BookingStatus.IN_PROGRESS);
    private static final Set<BookingStatus> TERMINAL_STATUSES = EnumSet.of(
            BookingStatus.DONE,
            BookingStatus.CANCELLED);

    private final BookingRepository bookingRepository;
    private final CustomerRepository customerRepository;
    private final VehicleRepository vehicleRepository;
    private final NotificationService notificationService;
    private final LoyaltyService loyaltyService;
    private final PromotionRepository promotionRepository;

    public BookingResponse createBooking(
            CreateBookingRequest request,
            UserDetails userDetails) {

        Customer customer;
        Vehicle vehicle;

        // Luồng 1: Khách hàng tự đặt lịch từ giao diện cá nhân
        if (userDetails != null) {
            customer = findCurrentCustomer(userDetails);
            vehicle = vehicleRepository
                    .findByVehicleIdAndCustomer_CustomerId(
                            request.getVehicleId(), customer.getCustomerId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Không tìm thấy xe thuộc tài khoản hiện tại"));
        } else {
            // Luồng 2: POS ADMIN LÊN ĐƠN TẠI QUẦY (TỰ ĐỘNG BÓC TÁCH TẠO XE VÀ KHÁCH)
            String phoneTarget = null;
            String plateTarget = null;
            String modelTarget = null;

            if (request.getNotes() != null && request.getNotes().contains("Phone:")) {
                try {
                    // Cấu trúc chuỗi notes: POS Admin: Khách... | Phone:... | Biển số:... | Dòng
                    // xe:...
                    String[] parts = request.getNotes().split(" \\| ");
                    for (String part : parts) {
                        if (part.trim().startsWith("Phone:")) {
                            phoneTarget = part.replace("Phone:", "").trim();
                        } else if (part.trim().startsWith("Biển số:")) {
                            plateTarget = part.replace("Biển số:", "").trim();
                        } else if (part.trim().startsWith("Dòng xe:")) {
                            modelTarget = part.replace("Dòng xe:", "").trim();
                        }
                    }
                } catch (Exception e) {
                    System.out.println("[POS] Lỗi trích xuất thông tin chi tiết từ trường ghi chú notes");
                }
            }

            // 1. Định vị khách hàng mục tiêu qua Số điện thoại
            if (phoneTarget != null && !phoneTarget.isEmpty()) {
                customer = customerRepository.findByPhone(phoneTarget)
                        .orElseGet(() -> {
                            System.out.println("[POS] Số điện thoại lạ, gán tạm vào khách xe mặc định");
                            return vehicleRepository.findById(request.getVehicleId())
                                    .map(Vehicle::getCustomer)
                                    .orElse(null);
                        });
            } else {
                customer = vehicleRepository.findById(request.getVehicleId())
                        .map(Vehicle::getCustomer)
                        .orElse(null);
            }

            // 2. TỰ ĐỘNG KIỂM TRA BIỂN SỐ VÀ KHỞI TẠO THỰC THỂ XE THẬT DƯỚI DATABASE
            if (plateTarget != null && !plateTarget.isEmpty()) {
                final String finalPlate = plateTarget.toUpperCase().trim();
                final String finalBrand = (modelTarget != null && !modelTarget.isEmpty()) ? modelTarget : "POS Auto";
                final Customer finalCustomer = customer;

                // Tìm kiếm xe theo biển số đã được chuẩn hóa
                vehicle = vehicleRepository.findAll().stream()
                        .filter(v -> v.getLicensePlate() != null
                                && v.getLicensePlate().toUpperCase().trim().equals(finalPlate))
                        .findFirst()
                        .orElseGet(() -> {
                            System.out.println(
                                    "[POS] Xe mang biển số " + finalPlate + " chưa có trong hệ thống. Đang tạo mới...");
                            Vehicle newVehicle = Vehicle.builder()
                                    .licensePlate(finalPlate)
                                    .brand(finalBrand)
                                    .color("Mặc định")
                                    .vehicleType("CAR")
                                    .customer(finalCustomer)
                                    .isPrimary(false)
                                    .build();
                            return vehicleRepository.save(newVehicle);
                        });
            } else {
                // Phương án dự phòng bypass nếu lỗi trống biển số
                vehicle = vehicleRepository.findById(request.getVehicleId())
                        .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy xe hệ thống mặc định"));
            }
        }

        if (customer == null) {
            throw new BusinessException("Không xác định được khách hàng hợp lệ cho lịch đặt này");
        }

        LocalDateTime scheduledAt = request.getScheduledAt()
                .withSecond(0)
                .withNano(0);
        validateSchedulableSlot(customer, scheduledAt);

        if (request.getServiceIds() == null || request.getServiceIds().isEmpty()) {
            throw new BusinessException("Vui lòng chọn ít nhất một dịch vụ để đặt lịch");
        }

        java.util.List<WashService> selectedServices = washServiceRepository.findAllById(request.getServiceIds());
        if (selectedServices.isEmpty()) {
            throw new BusinessException("Các dịch vụ được chọn không hợp lệ hoặc không tồn tại");
        }

        BigDecimal total = BigDecimal.ZERO;
        for (WashService s : selectedServices) {
            total = total.add(s.getBasePrice());
        }

        BigDecimal discount = BigDecimal.ZERO;
        Promotion promotion = null;
        if (request.getPromoId() != null) {
            promotion = promotionRepository.findById(request.getPromoId())
                    .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy chương trình khuyến mãi"));

            if (!promotion.isActive()) {
                throw new BusinessException("Chương trình khuyến mãi không hoạt động");
            }
            LocalDateTime now = LocalDateTime.now();
            if (now.isBefore(promotion.getStartsAt()) || now.isAfter(promotion.getEndsAt())) {
                throw new BusinessException("Chương trình khuyến mãi đã hết hạn hoặc chưa bắt đầu");
            }
            if (promotion.getUsageLimit() != null && promotion.getUsageCount() >= promotion.getUsageLimit()) {
                throw new BusinessException("Chương trình khuyến mãi đã hết lượt sử dụng");
            }
            String userTierName = customer.getTier().name();
            String targetTiers = promotion.getTargetTiers();
            if (targetTiers != null && !targetTiers.contains(userTierName)) {
                throw new BusinessException("Hạng thành viên của bạn không đủ điều kiện sử dụng mã này");
            }

            if (promotion.getPromoType() == com.autowash.autowash_pro.enums.PromoType.DISCOUNT) {
                BigDecimal promoVal = promotion.getValue();
                if (promoVal.compareTo(BigDecimal.valueOf(100)) <= 0) {
                    discount = total.multiply(promoVal).divide(BigDecimal.valueOf(100), 2, java.math.RoundingMode.HALF_UP);
                } else {
                    discount = promoVal;
                }
            } else if (promotion.getPromoType() == com.autowash.autowash_pro.enums.PromoType.FREE_WASH) {
                discount = total;
            } else if (promotion.getPromoType() == com.autowash.autowash_pro.enums.PromoType.ADD_ON) {
                discount = promotion.getValue();
            }

            if (discount.compareTo(total) > 0) {
                discount = total;
            }

            promotion.setUsageCount(promotion.getUsageCount() + 1);
            promotionRepository.save(promotion);
        }

        BigDecimal finalTotal = total.subtract(discount);
        if (finalTotal.compareTo(BigDecimal.ZERO) < 0) {
            finalTotal = BigDecimal.ZERO;
        }

        Booking booking = Booking.builder()
                .customer(customer)
                .vehicle(vehicle)
                .scheduledAt(scheduledAt)
                .status(BookingStatus.PENDING)
                .priorityScore(customer.getTier().getPriorityScore())
                .notes(request.getNotes())
                .totalAmount(finalTotal)
                .promotion(promotion)
                .discountAmount(discount)
                .build();

        Booking savedBooking = bookingRepository.save(booking);

        java.util.List<BookingWashService> bookingServicesList = new java.util.ArrayList<>();
        for (WashService ws : selectedServices) {
            BookingWashService bws = BookingWashService.builder()
                    .booking(savedBooking)
                    .washService(ws)
                    .quantity(1)
                    .unitPrice(ws.getBasePrice())
                    .duration(ws.getEstimatedDuration())
                    .subtotal(ws.getBasePrice())
                    .build();
            bookingServicesList.add(bookingWashServiceRepository.save(bws));
        }
        savedBooking.setBookingServices(bookingServicesList);

        return mapToResponse(savedBooking);
    }

    @Transactional(readOnly = true)
    public List<BookingResponse> getMyBookings(UserDetails userDetails) {
        Customer customer = findCurrentCustomer(userDetails);
        return bookingRepository
                .findByCustomer_CustomerIdOrderByScheduledAtDesc(
                        customer.getCustomerId())
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public BookingResponse getBooking(UUID bookingId, UserDetails userDetails) {
        Booking booking = findBooking(bookingId);
        assertCanAccess(booking, userDetails);
        return mapToResponse(booking);
    }

    @Transactional(readOnly = true)
    public List<BookingResponse> getAdminBookings(
            BookingStatus status,
            LocalDate date) {

        if (date != null) {
            LocalDateTime from = date.atStartOfDay();
            LocalDateTime to = date.plusDays(1).atStartOfDay();
            return bookingRepository
                    .findByScheduledAtBetweenOrderByScheduledAtDesc(
                            from, to)
                    .stream()
                    .filter(booking -> status == null
                            || booking.getStatus() == status)
                    .map(this::mapToResponse)
                    .toList();
        }

        if (status != null) {
            return bookingRepository.findByStatusOrderByScheduledAtDesc(status)
                    .stream()
                    .map(this::mapToResponse)
                    .toList();
        }

        return bookingRepository.findAllByOrderByScheduledAtDesc().stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<AvailabilitySlotResponse> getAvailability(
            LocalDate date,
            UserDetails userDetails) {

        Customer customer = findCurrentCustomer(userDetails);
        LocalDateTime firstSlot = date.atTime(OPEN_TIME);
        LocalDateTime closeAt = date.atTime(CLOSE_TIME);

        return Stream.iterate(
                firstSlot,
                slot -> slot.isBefore(closeAt),
                slot -> slot.plusMinutes(SLOT_MINUTES))
                .map(slot -> buildAvailabilitySlot(slot, customer))
                .toList();
    }

    public BookingResponse cancelBooking(
            UUID bookingId,
            UserDetails userDetails) {

        Booking booking = findBooking(bookingId);
        boolean admin = isAdmin(userDetails);
        if (!admin) {
            assertCanAccess(booking, userDetails);
        }

        if (booking.getStatus() == BookingStatus.DONE) {
            throw new BusinessException("Không thể hủy lịch đã hoàn tất");
        }
        if (booking.getStatus() == BookingStatus.CANCELLED) {
            return mapToResponse(booking);
        }
        if (!admin && booking.getStatus() == BookingStatus.IN_PROGRESS) {
            throw new BusinessException("Lịch đang rửa không thể tự hủy");
        }

        booking.setStatus(BookingStatus.CANCELLED);
        Booking savedBooking = bookingRepository.save(booking);
        notificationService.sendBookingStatusChanged(savedBooking);
        return mapToResponse(savedBooking);
    }

    public BookingResponse updateStatus(
            UUID bookingId,
            BookingStatus newStatus) {

        Booking booking = findBooking(bookingId);
        BookingStatus oldStatus = booking.getStatus();

        if (oldStatus == BookingStatus.DONE) {
            return mapToResponse(booking);
        }

        validateStatusTransition(oldStatus, newStatus);
        booking.setStatus(newStatus);

        Booking savedBooking = bookingRepository.save(booking);
        notificationService.sendBookingStatusChanged(savedBooking);
        if (oldStatus != BookingStatus.DONE && newStatus == BookingStatus.DONE) {
            if (savedBooking.getCustomer() != null) {

                BigDecimal amountPaid = savedBooking.getTotalAmount() != null 
                        ? savedBooking.getTotalAmount() 
                        : (savedBooking.getServiceType() != null 
                            ? BigDecimal.valueOf(savedBooking.getServiceType().getBasePrice()) 
                            : new BigDecimal("50000"));

                int totalPoints = 0;
                if (savedBooking.getBookingServices() != null) {
                    for (BookingWashService bws : savedBooking.getBookingServices()) {
                        if (bws.getWashService() != null && bws.getWashService().getPoints() != null) {
                            totalPoints += bws.getWashService().getPoints() * bws.getQuantity();
                        }
                    }
                }

                com.autowash.autowash_pro.dto.request.loyalty.EarnPointsRequest earnRequest = new com.autowash.autowash_pro.dto.request.loyalty.EarnPointsRequest();
                earnRequest.setCustomerId(savedBooking.getCustomer().getCustomerId());
                earnRequest.setWashId(savedBooking.getBookingId());
                earnRequest.setAmountPaid(amountPaid);
                earnRequest.setCustomPoints(totalPoints > 0 ? totalPoints : null);

                // 1. Thực hiện tích điểm loyalty trước
                loyaltyService.earnPoints(earnRequest);

                // 2. Tự động thăng hạng thời gian thực sau khi nạp điểm thành công
                Customer currentCustomer = savedBooking.getCustomer();
                Customer updatedCustomer = customerRepository.findById(currentCustomer.getCustomerId())
                        .orElse(currentCustomer);

                int currentPoints = updatedCustomer.getLifetimePoints();
                Tier newTier = Tier.getNextTierFromPoints(currentPoints);

                if (updatedCustomer.getTier() != newTier) {
                    updatedCustomer.setTier(newTier);
                    customerRepository.save(updatedCustomer);
                    System.out.println("[Promotion] Khách hàng " + updatedCustomer.getFullName()
                            + " đã tự động thăng hạng lên: " + newTier);
                }

                System.out.println("[Automation] Đơn đặt lịch " + bookingId
                        + " hoàn tất. Đã tự động tích điểm loyalty về đúng ID khách.");
            }
        }

        return mapToResponse(savedBooking);
    }

    private AvailabilitySlotResponse buildAvailabilitySlot(
            LocalDateTime slot,
            Customer customer) {

        int booked = bookingRepository
                .countByScheduledAtAndStatusIn(slot, ACTIVE_STATUSES);
        int remaining = Math.max(0, SLOT_CAPACITY - booked);

        boolean validForCustomer = isWithinBookingWindow(customer, slot)
                && slot.isAfter(LocalDateTime.now());

        return AvailabilitySlotResponse.builder()
                .startsAt(slot)
                .bookedCount(booked)
                .remainingCapacity(remaining)
                .available(validForCustomer && remaining > 0)
                .build();
    }

    private void validateSchedulableSlot(
            Customer customer,
            LocalDateTime scheduledAt) {

        if (scheduledAt.getMinute() % SLOT_MINUTES != 0
                || scheduledAt.getSecond() != 0) {
            throw new BusinessException(
                    "Slot đặt lịch phải theo mốc 30 phút");
        }

        LocalTime time = scheduledAt.toLocalTime();
        if (time.isBefore(OPEN_TIME) || !time.isBefore(CLOSE_TIME)) {
            throw new BusinessException(
                    "Chỉ nhận lịch trong khung 08:00 - 18:00");
        }

        if (!scheduledAt.isAfter(LocalDateTime.now().plusMinutes(30))) {
            throw new BusinessException(
                    "Vui lòng đặt lịch trước ít nhất 30 phút");
        }

        if (!isWithinBookingWindow(customer, scheduledAt)) {
            throw new BusinessException(String.format(
                    "Hạng %s chỉ được đặt lịch trong %d ngày tới",
                    customer.getTier(),
                    customer.getTier().getBookingWindowDays()));
        }

        int booked = bookingRepository
                .countByScheduledAtAndStatusIn(scheduledAt, ACTIVE_STATUSES);
        if (booked >= SLOT_CAPACITY) {
            throw new BusinessException(
                    "Khung giờ này đã hết chỗ, vui lòng chọn slot khác");
        }
    }

    private boolean isWithinBookingWindow(
            Customer customer,
            LocalDateTime scheduledAt) {
        return !scheduledAt.isAfter(
                LocalDateTime.now()
                        .plusDays(customer.getTier().getBookingWindowDays()));
    }

    private void validateStatusTransition(
            BookingStatus currentStatus,
            BookingStatus newStatus) {

        if (currentStatus == newStatus) {
            return;
        }
        if (TERMINAL_STATUSES.contains(currentStatus)) {
            throw new BusinessException(
                    "Không thể đổi trạng thái của booking đã kết thúc");
        }

        boolean valid = switch (currentStatus) {
            case PENDING -> newStatus == BookingStatus.CONFIRMED
                    || newStatus == BookingStatus.CANCELLED;
            case CONFIRMED -> newStatus == BookingStatus.IN_PROGRESS
                    || newStatus == BookingStatus.CANCELLED;
            case IN_PROGRESS -> newStatus == BookingStatus.DONE;
            default -> false;
        };

        if (!valid) {
            throw new BusinessException(String.format(
                    "Không thể đổi trạng thái từ %s sang %s",
                    currentStatus, newStatus));
        }
    }

    private Booking findBooking(UUID bookingId) {
        return bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Không tìm thấy booking: " + bookingId));
    }

    private Customer findCurrentCustomer(UserDetails userDetails) {
        return customerRepository.findByPhone(userDetails.getUsername())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Không tìm thấy tài khoản hiện tại"));
    }

    private void assertCanAccess(
            Booking booking,
            UserDetails userDetails) {

        if (isAdmin(userDetails)) {
            return;
        }

        Customer customer = findCurrentCustomer(userDetails);
        if (!booking.getCustomer().getCustomerId()
                .equals(customer.getCustomerId())) {
            throw new BusinessException(
                    "Bạn không có quyền truy cập booking này");
        }
    }

    private boolean isAdmin(UserDetails userDetails) {
        if (userDetails == null)
            return false;
        return userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch("ROLE_ADMIN"::equals);
    }

    private BookingResponse mapToResponse(Booking booking) {
        return BookingResponse.from(booking);
    }
}