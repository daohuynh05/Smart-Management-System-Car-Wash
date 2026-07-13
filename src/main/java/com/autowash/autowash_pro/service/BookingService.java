package com.autowash.autowash_pro.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
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
import com.autowash.autowash_pro.dto.request.loyalty.EarnPointsRequest;
import com.autowash.autowash_pro.dto.response.booking.AvailabilitySlotResponse;
import com.autowash.autowash_pro.dto.response.booking.BookingResponse;
import com.autowash.autowash_pro.entity.Booking;
import com.autowash.autowash_pro.entity.Customer;
import com.autowash.autowash_pro.entity.CustomerPoints;
import com.autowash.autowash_pro.entity.Vehicle;
import com.autowash.autowash_pro.entity.WashHistory;
import com.autowash.autowash_pro.entity.WashService;
import com.autowash.autowash_pro.entity.BookingWashService;
import com.autowash.autowash_pro.enums.BookingStatus;
import com.autowash.autowash_pro.enums.PointType;
import com.autowash.autowash_pro.enums.Tier;
import com.autowash.autowash_pro.enums.ServiceType;
import com.autowash.autowash_pro.exception.BusinessException;
import com.autowash.autowash_pro.exception.ResourceNotFoundException;
import com.autowash.autowash_pro.repository.BookingRepository;
import com.autowash.autowash_pro.repository.CustomerRepository;
import com.autowash.autowash_pro.repository.PromotionRepository;
import com.autowash.autowash_pro.repository.VehicleRepository;
import com.autowash.autowash_pro.repository.WashHistoryRepository;
import com.autowash.autowash_pro.repository.WashServiceRepository;
import com.autowash.autowash_pro.repository.BookingWashServiceRepository;
import com.autowash.autowash_pro.repository.CustomerPointsRepository;
import com.autowash.autowash_pro.entity.Promotion;
import com.autowash.autowash_pro.entity.SystemConfig;
import com.autowash.autowash_pro.entity.TierRule;

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
    private final WashHistoryRepository washHistoryRepository;
    private final PromotionRepository promotionRepository;
    private final NotificationService notificationService;
    private final LoyaltyService loyaltyService;
    private final CustomerPointsRepository customerPointsRepository;
    private final AdminConfigService adminConfigService;

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

        List<WashService> selectedServices = washServiceRepository.findAllById(request.getServiceIds());
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
            int userRank = switch (customer.getTier()) {
                case MEMBER -> 1;
                case SILVER -> 2;
                case GOLD -> 3;
                case PLATINUM -> 4;
            };
            int minRequiredRank = 4;
            String targetTiers = promotion.getTargetTiers();
            if (targetTiers != null && !targetTiers.trim().isEmpty()) {
                for (String t : targetTiers.split(",")) {
                    try {
                        Tier targetTier = Tier.valueOf(t.trim().toUpperCase());
                        int rank = switch (targetTier) {
                            case MEMBER -> 1;
                            case SILVER -> 2;
                            case GOLD -> 3;
                            case PLATINUM -> 4;
                        };
                        if (rank < minRequiredRank) {
                            minRequiredRank = rank;
                        }
                    } catch (Exception e) {
                        // ignore malformed tier
                    }
                }
            } else {
                minRequiredRank = 1;
            }
            if (userRank < minRequiredRank) {
                throw new BusinessException("Hạng thành viên của bạn không đủ điều kiện sử dụng mã này");
            }

            // Kiểm tra xem khách hàng này đã dùng mã khuyến mãi này trong một booking hợp
            // lệ nào chưa
            if (bookingRepository.existsByCustomer_CustomerIdAndPromotion_PromoIdAndStatusNot(
                    customer.getCustomerId(), promotion.getPromoId(), BookingStatus.CANCELLED)) {
                throw new BusinessException("Bạn đã sử dụng chương trình khuyến mãi này rồi");
            }

            BigDecimal promoVal = promotion.getValue();
            if (promoVal.compareTo(BigDecimal.valueOf(100)) <= 0) {
                discount = total.multiply(promoVal).divide(BigDecimal.valueOf(100), 2, java.math.RoundingMode.HALF_UP);
                if (promotion.getMaxDiscount() != null && discount.compareTo(promotion.getMaxDiscount()) > 0) {
                    discount = promotion.getMaxDiscount();
                }
            } else {
                discount = promoVal;
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

        // --- Xử lý đổi điểm tích lũy thành tiền mặt (1 điểm = 100đ) ---
        int pointsToUse = 0;
        BigDecimal pointsDiscount = BigDecimal.ZERO;
        if (request.getUsedPoints() > 0) {
            if (customer.getTotalPoints() < request.getUsedPoints()) {
                throw new BusinessException(String.format(
                        "Không đủ điểm tích lũy. Bạn chọn dùng %d điểm nhưng chỉ có %d điểm",
                        request.getUsedPoints(), customer.getTotalPoints()));
            }

            // Tính toán giá trị giảm trừ tối đa không vượt quá finalTotal
            BigDecimal potentialPointsDiscount = BigDecimal.valueOf(request.getUsedPoints() * 100L);
            if (potentialPointsDiscount.compareTo(finalTotal) > 0) {
                // Khấu trừ toàn bộ hóa đơn về 0đ
                pointsDiscount = finalTotal;
                // Quy ngược lại số điểm cần trừ chẵn (làm tròn lên để đủ số tiền cần trừ)
                pointsToUse = (int) Math.ceil(finalTotal.doubleValue() / 100.0);
                if (pointsToUse > customer.getTotalPoints()) {
                    pointsToUse = customer.getTotalPoints();
                }
                pointsDiscount = BigDecimal.valueOf(pointsToUse * 100L);
            } else {
                pointsToUse = request.getUsedPoints();
                pointsDiscount = potentialPointsDiscount;
            }

            if (pointsToUse > 0) {
                // Thực hiện trừ điểm của khách hàng theo FIFO
                loyaltyService.deductPointsFifo(customer, pointsToUse);

                // Ghi nhận dòng log REDEEM điểm vào lịch sử
                CustomerPoints redeemLog = CustomerPoints
                        .builder()
                        .customer(customer)
                        .type(PointType.REDEEM)
                        .points(-pointsToUse)
                        .balanceAfter(customer.getTotalPoints())
                        .referenceId(UUID.randomUUID())
                        .description("Khấu trừ đặt lịch #" + scheduledAt.toLocalDate())
                        .expiresAt(LocalDateTime.now().plusYears(99))
                        .build();

                customerPointsRepository.save(redeemLog);
                customerRepository.save(customer);
            }
        }

        finalTotal = finalTotal.subtract(pointsDiscount);
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
                .usedPoints(pointsToUse)
                .pointsDiscountAmount(pointsDiscount)
                .build();

        Booking savedBooking = bookingRepository.save(booking);

        List<BookingWashService> bookingServicesList = new ArrayList<>();
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

        int bookingWindowDays = getBookingWindowDays(customer);

        return Stream.iterate(
                firstSlot,
                slot -> slot.isBefore(closeAt),
                slot -> slot.plusMinutes(SLOT_MINUTES))
                .map(slot -> buildAvailabilitySlot(slot, customer, bookingWindowDays))
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

        // Hoàn điểm nếu booking có sử dụng điểm khấu trừ
        if (booking.getUsedPoints() > 0 && booking.getCustomer() != null) {
            Customer customer = booking.getCustomer();
            customer.setTotalPoints(customer.getTotalPoints() + booking.getUsedPoints());
            customer.setLifetimePoints(customer.getLifetimePoints() + booking.getUsedPoints()); // Khôi phục lại
                                                                                                // lifetime
            customerRepository.save(customer);

            CustomerPoints refundLog = com.autowash.autowash_pro.entity.CustomerPoints.builder()
                    .customer(customer)
                    .type(PointType.EARN)
                    .points(booking.getUsedPoints())
                    .balanceAfter(customer.getTotalPoints())
                    .referenceId(booking.getBookingId())
                    .description("Hoàn điểm hủy lịch #" + booking.getScheduledAt().toLocalDate())
                    .expiresAt(LocalDateTime.now().plusMonths(12)) // Điểm hoàn hạn dùng 12 tháng mặc định
                    .build();
            customerPointsRepository.save(refundLog);
        }

        booking.setStatus(BookingStatus.CANCELLED);
        Booking savedBooking = bookingRepository.save(booking);
        notificationService.sendBookingStatusChanged(savedBooking);
        return mapToResponse(savedBooking);
    }

    @Transactional
    public BookingResponse updateStatus(
            UUID bookingId,
            BookingStatus newStatus) {
        return updateStatus(bookingId, newStatus, null);
    }

    @Transactional
    public BookingResponse updateStatus(
            UUID bookingId,
            BookingStatus newStatus,
            UUID promoId) {

        Booking booking = findBooking(bookingId);
        validateStatusTransition(booking.getStatus(), newStatus);

        if (newStatus == BookingStatus.DONE) {
            // Kiểm tra xem đã checkout chưa để tránh trùng lặp
            if (washHistoryRepository.existsByBooking_BookingId(bookingId)) {
                throw new BusinessException("Lịch đặt này đã được hoàn tất trước đó.");
            }

            BigDecimal basePrice = booking.getTotalAmount() != null
                    ? booking.getTotalAmount()
                    : (booking.getServiceType() != null
                            ? BigDecimal.valueOf(booking.getServiceType().getBasePrice())
                            : BigDecimal.valueOf(50000));
            BigDecimal discountApplied = BigDecimal.ZERO;
            Promotion promotion = null;

            if (promoId != null) {
                promotion = promotionRepository.findById(promoId)
                        .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy chương trình khuyến mãi"));

                if (!promotion.isValid()) {
                    throw new BusinessException("Chương trình khuyến mãi đã hết hạn hoặc hết lượt sử dụng");
                }

                if (!promotion.getTargetTierList().contains(booking.getCustomer().getTier())) {
                    throw new BusinessException(
                            "Hạng thành viên của bạn không được áp dụng chương trình khuyến mãi này");
                }

                LocalDateTime sevenDaysAgo = LocalDateTime.now().minusDays(7);
                boolean alreadyUsed = washHistoryRepository
                        .existsByCustomer_CustomerIdAndPromo_PromoTypeAndWashedAtAfter(
                                booking.getCustomer().getCustomerId(),
                                promotion.getPromoType(),
                                sevenDaysAgo);
                if (alreadyUsed) {
                    throw new BusinessException("Bạn đã sử dụng chương trình khuyến mãi loại "
                            + promotion.getPromoType() + " trong vòng 7 ngày qua");
                }

                discountApplied = promotion.getValue();
                if (discountApplied.compareTo(basePrice) > 0) {
                    discountApplied = basePrice;
                }

                // Tăng usage_count sau khi áp dụng
                promotion.setUsageCount(promotion.getUsageCount() + 1);
                promotionRepository.save(promotion);
            } else if (booking.getPromotion() != null) {
                promotion = booking.getPromotion();
                discountApplied = booking.getDiscountAmount() != null ? booking.getDiscountAmount() : BigDecimal.ZERO;
            }

            BigDecimal pointsDiscount = booking.getPointsDiscountAmount() != null ? booking.getPointsDiscountAmount()
                    : BigDecimal.ZERO;
            BigDecimal amountPaid = basePrice.subtract(discountApplied).subtract(pointsDiscount);
            if (amountPaid.compareTo(BigDecimal.ZERO) < 0) {
                amountPaid = BigDecimal.ZERO;
            }

            // Tạo và lưu WashHistory
            WashHistory washHistory = WashHistory.builder()
                    .customer(booking.getCustomer())
                    .vehicle(booking.getVehicle())
                    .booking(booking)
                    .washedAt(LocalDateTime.now())
                    .serviceType(booking.getServiceType() != null ? booking.getServiceType() : ServiceType.BASIC)
                    .amountPaid(amountPaid)
                    .discountApplied(discountApplied)
                    .promo(promotion)
                    .build();

            washHistoryRepository.save(washHistory);
        }

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

                EarnPointsRequest earnRequest = new EarnPointsRequest();
                earnRequest.setCustomerId(savedBooking.getCustomer().getCustomerId());
                earnRequest.setWashId(savedBooking.getBookingId());
                earnRequest.setAmountPaid(amountPaid);
                earnRequest.setCustomPoints(totalPoints > 0 ? totalPoints : null);

                // 1. Thực hiện tích điểm loyalty trước (đã tự động tính toán thăng hạng theo
                // lượt rửa xe bên trong)
                loyaltyService.earnPoints(earnRequest);

                System.out.println("[Automation] Đơn đặt lịch " + bookingId
                        + " hoàn tất. Đã tự động tích điểm loyalty về đúng ID khách.");
            }
        }

        return mapToResponse(savedBooking);
    }

    private int getBookingWindowDays(Customer customer) {
        int bookingWindowDays = customer.getTier().getBookingWindowDays();
        try {
            SystemConfig config = adminConfigService.getSystemConfig();
            if (config != null && config.getTierRules() != null) {
                TierRule rule = config.getTierRules().stream()
                        .filter(r -> r.getTier().equalsIgnoreCase(customer.getTier().name()))
                        .findFirst()
                        .orElse(null);
                if (rule != null) {
                    bookingWindowDays = rule.getBookingWindow();
                }
            }
        } catch (Exception e) {
            // Dự phòng
        }
        return bookingWindowDays;
    }

    private AvailabilitySlotResponse buildAvailabilitySlot(
            LocalDateTime slot,
            Customer customer,
            int bookingWindowDays) {

        int booked = bookingRepository
                .countByScheduledAtAndStatusIn(slot, ACTIVE_STATUSES);
        int remaining = Math.max(0, SLOT_CAPACITY - booked);

        boolean validForCustomer = !slot.isAfter(LocalDateTime.now().plusDays(bookingWindowDays))
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

        int bookingWindowDays = getBookingWindowDays(customer);
        if (!isWithinBookingWindow(customer, scheduledAt, bookingWindowDays)) {
            throw new BusinessException(String.format(
                    "Hạng %s chỉ được đặt lịch trong %d ngày tới",
                    customer.getTier(),
                    bookingWindowDays));
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
            LocalDateTime scheduledAt,
            int bookingWindowDays) {
        return !scheduledAt.isAfter(
                LocalDateTime.now().plusDays(bookingWindowDays));
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
                .map(auth -> auth.getAuthority())
                .anyMatch("ROLE_ADMIN"::equals);
    }

    private BookingResponse mapToResponse(Booking booking) {
        return BookingResponse.from(booking);
    }
}