package com.autowash.autowash_pro.service;

import com.autowash.autowash_pro.dto.request.loyalty.EarnPointsRequest;
import com.autowash.autowash_pro.dto.request.loyalty.RedeemPointsRequest;
import com.autowash.autowash_pro.dto.response.loyalty.*;
import com.autowash.autowash_pro.entity.Customer;
import com.autowash.autowash_pro.entity.CustomerPoints;
import com.autowash.autowash_pro.enums.PointType;
import com.autowash.autowash_pro.enums.RedeemType;
import com.autowash.autowash_pro.enums.Tier;
import com.autowash.autowash_pro.exception.BusinessException;
import com.autowash.autowash_pro.exception.ResourceNotFoundException;
import com.autowash.autowash_pro.entity.SystemConfig;
import com.autowash.autowash_pro.entity.TierRule;
import com.autowash.autowash_pro.repository.CustomerPointsRepository;
import com.autowash.autowash_pro.repository.CustomerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class LoyaltyService {

    private final CustomerRepository customerRepository;
    private final CustomerPointsRepository customerPointsRepository;
    private final NotificationService notificationService;
    private final AdminConfigService adminConfigService;

    // =========================================================================
    // Loyalty: Tích điểm sau khi rửa xe xong
    // POST /api/loyalty/earn
    // =========================================================================

    public EarnPointsResponse earnPoints(EarnPointsRequest request) {
        Customer customer = findCustomerById(request.getCustomerId());
        SystemConfig systemConfig = adminConfigService.getSystemConfig();

        // Lấy tỷ lệ quy đổi từ SystemConfig
        String cleanRate = systemConfig.getPointRate().replace(".", "").replace(",", "").trim();
        java.math.BigDecimal vndPerPoint = new java.math.BigDecimal(cleanRate);

        int points;
        if (request.getCustomPoints() != null && request.getCustomPoints() > 0) {
            points = request.getCustomPoints();
        } else {
            // Tính điểm: (amountPaid / vndPerPoint) × tierMultiplier từ SystemConfig / TierRule
            double multiplier = getTierRuleMultiplier(systemConfig, customer.getTier());
            points = (int) (request.getAmountPaid()
                    .divide(vndPerPoint, 4, RoundingMode.FLOOR)
                    .doubleValue() * multiplier);
        }

        if (points <= 0) {
            log.debug("Giao dịch {} không đủ điều kiện tích điểm (amountPaid={})",
                    request.getWashId(), request.getAmountPaid());
            return EarnPointsResponse.builder()
                    .customerId(customer.getCustomerId())
                    .pointsEarned(0)
                    .newBalance(customer.getTotalPoints())
                    .newTier(customer.getTier().name())
                    .message("Số tiền quá nhỏ để tích điểm")
                    .build();
        }

        // Cập nhật số dư khách hàng
        int newBalance = customer.getTotalPoints() + points;
        customer.setTotalPoints(newBalance);
        customer.setLifetimePoints(customer.getLifetimePoints() + points);
        customer.setTotalVisits(customer.getTotalVisits() + 1);
        customer.setLastVisitAt(LocalDateTime.now());
        if (request.getAmountPaid() != null) {
            customer.setTotalSpend(customer.getTotalSpend().add(request.getAmountPaid()));
        }

        // Ghi log giao dịch điểm
        CustomerPoints pointLog = CustomerPoints.builder()
                .customer(customer)
                .type(PointType.EARN)
                .points(points)
                .balanceAfter(newBalance)
                .referenceId(request.getWashId())
                .description(String.format("Tích điểm rửa xe %.0f VND", request.getAmountPaid()))
                .expiresAt(LocalDateTime.now().plusMonths(12))
                .build();

        customerPointsRepository.save(pointLog);

        Tier oldTier = customer.getTier();
        checkAndUpdateTier(customer);
        customerRepository.save(customer);

        notificationService.sendPointsEarned(customer, points, customer.getTotalPoints());
        if (customer.getTier() != oldTier) {
            notificationService.sendTierChanged(customer, oldTier, customer.getTier());
        }

        log.info("Khách {} tích được {} điểm | amountPaid={} | tier={}",
                customer.getCustomerId(), points, request.getAmountPaid(), customer.getTier());

        return EarnPointsResponse.builder()
                .customerId(customer.getCustomerId())
                .pointsEarned(points)
                .newBalance(customer.getTotalPoints())
                .newTier(customer.getTier().name())
                .message(String.format("Tích thành công %d điểm", points))
                .build();
    }

    // =========================================================================
    // Loyalty: Đổi điểm lấy thưởng
    // POST /api/loyalty/redeem
    // =========================================================================

    public RedeemPointsResponse redeemPoints(RedeemPointsRequest request,
            String currentUserPhone) {
        validateCustomerAccess(request.getCustomerId(), currentUserPhone);
        Customer customer = findCustomerById(request.getCustomerId());

        int cost = request.getPoints();

        // Kiểm tra đủ điểm không
        if (customer.getTotalPoints() < cost) {
            throw new BusinessException(String.format(
                    "Không đủ điểm. Cần %d điểm, bạn chỉ có %d điểm",
                    cost, customer.getTotalPoints()));
        }

        // Trừ điểm theo FIFO — điểm sắp hết hạn nhất bị trừ trước
        deductPointsFifo(customer, cost);

        // Ghi log giao dịch REDEEM
        CustomerPoints redeemLog = CustomerPoints.builder()
                .customer(customer)
                .type(PointType.REDEEM)
                .points(-cost)
                .balanceAfter(customer.getTotalPoints())
                .referenceId(request.getReferenceId())
                .description("Thanh toán bằng điểm tích lũy")
                .expiresAt(LocalDateTime.now().plusYears(99)) // REDEEM không bao giờ expire
                .build();

        customerPointsRepository.save(redeemLog);
        customerRepository.save(customer);

        log.info("Khách {} đổi {} điểm", customer.getCustomerId(), cost);

        return RedeemPointsResponse.builder()
                .customerId(customer.getCustomerId())
                .pointsUsed(cost)
                .remainingBalance(customer.getTotalPoints())
                .message(String.format("Thanh toán bằng điểm thành công, đã trừ %d điểm", cost))
                .build();
    }

    // =========================================================================
    // Loyalty: Xem số dư điểm (Client)
    // GET /api/loyalty/balance/{customerId}
    // =========================================================================

    @Transactional(readOnly = true)
    public PointBalanceResponse getBalance(UUID customerId, String currentUserPhone) {
        validateCustomerAccess(customerId, currentUserPhone);
        Customer customer = findCustomerById(customerId);

        // Điểm sắp hết hạn trong 30 ngày tới
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime in30Days = now.plusDays(30);
        List<CustomerPoints> expiringList = customerPointsRepository.findExpiringEarnPoints(customerId, now, in30Days);

        int expiringPoints = expiringList.stream().mapToInt(CustomerPoints::getPoints).sum();
        LocalDateTime nearestExpiry = expiringList.isEmpty() ? null : expiringList.get(0).getExpiresAt();

        // Tính toán tiến trình thăng hạng dựa trên số lượt rửa xe (Visits) từ cấu hình
        // động
        SystemConfig systemConfig = adminConfigService.getSystemConfig();
        List<TierRule> rules = systemConfig.getTierRules();

        int silverThreshold = 10;
        int goldThreshold = 25;
        int platinumThreshold = 50;

        if (rules != null) {
            for (TierRule rule : rules) {
                if ("silver".equalsIgnoreCase(rule.getTier())) {
                    silverThreshold = rule.getThreshold();
                } else if ("gold".equalsIgnoreCase(rule.getTier())) {
                    goldThreshold = rule.getThreshold();
                } else if ("platinum".equalsIgnoreCase(rule.getTier())) {
                    platinumThreshold = rule.getThreshold();
                }
            }
        }

        int visits = customer.getTotalVisits();
        String nextTier = "";
        int remainingVisits = 0;
        if (customer.getTier() == Tier.MEMBER) {
            nextTier = "SILVER";
            remainingVisits = Math.max(0, silverThreshold - visits);
        } else if (customer.getTier() == Tier.SILVER) {
            nextTier = "GOLD";
            remainingVisits = Math.max(0, goldThreshold - visits);
        } else if (customer.getTier() == Tier.GOLD) {
            nextTier = "PLATINUM";
            remainingVisits = Math.max(0, platinumThreshold - visits);
        }

        return PointBalanceResponse.builder()
                .customerId(customerId)
                .fullName(customer.getFullName())
                .tier(customer.getTier().name())
                .currentPoints(customer.getTotalPoints())
                .lifetimePoints(customer.getLifetimePoints())
                .expiringPointsIn30Days(expiringPoints)
                .nearestExpiryDate(nearestExpiry)
                .totalVisits(visits)
                .nextTier(nextTier)
                .remainingVisits(remainingVisits)
                .silverThreshold(silverThreshold)
                .goldThreshold(goldThreshold)
                .platinumThreshold(platinumThreshold)
                .build();
    }

    // =========================================================================
    // Customer: Lịch sử điểm (phân trang)
    // GET /api/customers/{id}/points
    // =========================================================================

    @Transactional(readOnly = true)
    public Page<PointHistoryResponse> getPointHistory(UUID customerId,
            Pageable pageable,
            String currentUserPhone) {
        validateCustomerAccess(customerId, currentUserPhone);
        if (!customerRepository.existsById(customerId)) {
            throw new ResourceNotFoundException("Không tìm thấy khách hàng: " + customerId);
        }
        return customerPointsRepository
                .findByCustomer_CustomerIdOrderByCreatedAtDesc(customerId, pageable)
                .map(this::mapToPointHistoryResponse);
    }

    // =========================================================================
    // Admin / Cron: Trigger rà soát tier thủ công
    // POST /api/loyalty/tier-review
    // =========================================================================

    public void runTierReview() {
        log.info("[TierReview] Bắt đầu rà soát tier toàn bộ khách hàng...");
        List<Customer> customers = customerRepository.findAll();
        int upgraded = 0;

        for (Customer customer : customers) {
            Tier oldTier = customer.getTier();
            checkAndUpdateTier(customer);
            customerRepository.save(customer);
            if (customer.getTier() != oldTier) {
                upgraded++;
                notificationService.sendTierChanged(customer, oldTier, customer.getTier());
            }
        }

        log.info("[TierReview] Kết thúc: {} khách được nâng tier", upgraded);
    }

    // =========================================================================
    // Cron: Expire điểm hết hạn — chạy mỗi ngày lúc 00:00
    // =========================================================================

    @Scheduled(cron = "0 0 0 * * *")
    public void expireOldPoints() {
        List<CustomerPoints> expiredList = customerPointsRepository.findExpiredEarnPoints(PointType.EARN,
                LocalDateTime.now());

        for (CustomerPoints expired : expiredList) {
            Customer customer = expired.getCustomer();
            int deduct = Math.min(expired.getPoints(), customer.getTotalPoints());
            customer.setTotalPoints(customer.getTotalPoints() - deduct);

            // Ghi log EXPIRE
            CustomerPoints expireLog = CustomerPoints.builder()
                    .customer(customer)
                    .type(PointType.EXPIRE)
                    .points(-deduct)
                    .balanceAfter(customer.getTotalPoints())
                    .referenceId(expired.getPointId())
                    .description("Điểm hết hạn (tích ngày " + expired.getCreatedAt().toLocalDate() + ")")
                    .expiresAt(LocalDateTime.now())
                    .build();

            // Đánh dấu bản gốc points = 0 để không expire lại
            expired.setPoints(0);

            customerPointsRepository.save(expireLog);
            customerPointsRepository.save(expired);
            customerRepository.save(customer);
        }

        if (!expiredList.isEmpty()) {
            log.info("[ExpirePoints] Đã xử lý {} giao dịch điểm hết hạn", expiredList.size());
        }
    }

    // =========================================================================
    // Cron: Rà soát tier tự động — chạy lúc 01:00 mỗi ngày
    // =========================================================================

    @Scheduled(cron = "0 0 1 * * *")
    public void scheduledTierReview() {
        log.info("[ScheduledTierReview] Tự động rà soát tier mỗi ngày...");
        runTierReview();
    }

    // =========================================================================
    // Private helpers
    // =========================================================================

    private Customer findCustomerById(UUID customerId) {
        return customerRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Không tìm thấy khách hàng: " + customerId));
    }

    private Customer findCustomerByPhone(String phone) {
        return customerRepository.findByPhone(phone)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Không tìm thấy admin: " + phone));
    }

    private void validateCustomerAccess(UUID customerId, String currentUserPhone) {
        if (currentUserPhone == null || currentUserPhone.isBlank()) {
            throw new BusinessException("Không xác định được user hiện tại");
        }

        Customer currentUser = findCustomerByPhone(currentUserPhone);
        if (currentUser.isAdmin()) {
            return; // Admin có quyền xem và đổi điểm cho bất kỳ khách hàng nào
        }

        if (!currentUser.getCustomerId().equals(customerId)) {
            throw new BusinessException("Bạn không có quyền truy cập thông tin này");
        }
    }

    private double getTierRuleMultiplier(SystemConfig config, Tier tier) {
        if (config.getTierRules() != null) {
            for (TierRule rule : config.getTierRules()) {
                if (rule.getTier().equalsIgnoreCase(tier.name())) {
                    return rule.getMultiplier() / 100.0;
                }
            }
        }
        return switch (tier) {
            case SILVER -> 1.10;
            case GOLD -> 1.25;
            case PLATINUM -> 1.50;
            default -> 1.0;
        };
    }

    /**
     * Trừ điểm theo FIFO — điểm sắp hết hạn nhất bị trừ trước.
     * Đảm bảo khách không mất điểm còn hiệu lực lâu dài.
     */
    public void deductPointsFifo(Customer customer, int totalCost) {
        List<CustomerPoints> fifoList = customerPointsRepository
                .findActiveEarnPointsFifo(customer.getCustomerId(), LocalDateTime.now());

        int remaining = totalCost;
        for (CustomerPoints pointRecord : fifoList) {
            if (remaining <= 0)
                break;
            int use = Math.min(pointRecord.getPoints(), remaining);
            pointRecord.setPoints(pointRecord.getPoints() - use);
            remaining -= use;
            customerPointsRepository.save(pointRecord);
        }

        customer.setTotalPoints(customer.getTotalPoints() - totalCost);
    }

    /**
     * Kiểm tra và nâng tier dựa trên totalVisits.
     * Ghi chú: trong thực tế nên tính số lần rửa trong 12 tháng gần nhất,
     * nhưng cần thêm query từ WashHistory (Dev 3 quản lý).
     * TODO: [Dev-4] Đổi sang dùng countVisitsInLast12Months() khi Dev 3 sẵn sàng.
     */
    private void checkAndUpdateTier(Customer customer) {
        int visits = customer.getTotalVisits();

        // Nạp động các ngưỡng từ cấu hình hệ thống
        SystemConfig config = adminConfigService.getSystemConfig();
        List<TierRule> rules = config.getTierRules();

        int silverThreshold = 10;
        int goldThreshold = 25;
        int platinumThreshold = 50;

        if (rules != null) {
            for (TierRule rule : rules) {
                if ("silver".equalsIgnoreCase(rule.getTier())) {
                    silverThreshold = rule.getThreshold();
                } else if ("gold".equalsIgnoreCase(rule.getTier())) {
                    goldThreshold = rule.getThreshold();
                } else if ("platinum".equalsIgnoreCase(rule.getTier())) {
                    platinumThreshold = rule.getThreshold();
                }
            }
        }

        Tier newTier = visits >= platinumThreshold ? Tier.PLATINUM
                : visits >= goldThreshold ? Tier.GOLD
                        : visits >= silverThreshold ? Tier.SILVER
                                : Tier.MEMBER;

        if (newTier != customer.getTier()) {
            log.info("Khách {} đổi tier {} → {}",
                    customer.getCustomerId(), customer.getTier(), newTier);
            customer.setTier(newTier);
        }
    }

    // ─── Mappers ──────────────────────────────────────────────────────────────

    private PointHistoryResponse mapToPointHistoryResponse(CustomerPoints points) {
        return PointHistoryResponse.builder()
                .pointId(points.getPointId())
                .type(points.getType())
                .points(points.getPoints())
                .balanceAfter(points.getBalanceAfter())
                .description(points.getDescription())
                .referenceId(points.getReferenceId())
                .expiresAt(points.getExpiresAt())
                .createdAt(points.getCreatedAt())
                .build();
    }
}
