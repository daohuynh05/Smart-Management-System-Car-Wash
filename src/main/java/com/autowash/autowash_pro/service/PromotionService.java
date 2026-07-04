package com.autowash.autowash_pro.service;

import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;

import com.autowash.autowash_pro.entity.Promotion;
import com.autowash.autowash_pro.dto.request.promotion.PromotionRequest;
import com.autowash.autowash_pro.repository.PromotionRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PromotionService {

    private final PromotionRepository promotionRepository;

    // 1. 🌟 HÀM ĐÃ NÂNG CẤP: Lấy danh sách kết hợp bộ lọc Tab và Tìm kiếm từ khóa
    // thông minh
    public List<Promotion> getPromotionsByStatus(String status, String keyword) {
        // 1. Nếu Admin điền từ khóa vào ô Search trên Topbar -> Ưu tiên tìm kiếm theo
        // Tên chương trình trước
        if (keyword != null && !keyword.trim().isEmpty()) {
            return promotionRepository.searchPromotions(keyword.trim());
        }

        // 2. Nếu ô Search rỗng -> Tiến hành lọc phân loại theo Tab như cũ
        if ("ACTIVE".equalsIgnoreCase(status)) {
            return promotionRepository.findActivePromotions(LocalDateTime.now());
        }

        if ("EXPIRED".equalsIgnoreCase(status)) {
            return promotionRepository.findExpiredPromotions(LocalDateTime.now());
        }

        // Mặc định cho Tab "Tất cả" (ALL)
        return promotionRepository.findAll();
    }

    // 2. API TẠO KHUYẾN MÃI
    public Promotion createPromotion(PromotionRequest request) {
        Promotion promotion = new Promotion();

        promotion.setName(request.getName());

        // Ép chuỗi String thành Enum tương ứng trong Entity
        if (request.getPromoType() != null) {
            promotion.setPromoType(com.autowash.autowash_pro.enums.PromoType.valueOf(request.getPromoType()));
        }

        promotion.setValue(request.getValue());
        promotion.setMaxDiscount(request.getMaxDiscount());
        promotion.setUsageLimit(request.getUsageLimit());

        promotion.setTargetTiers(request.getTargetTiers());
        promotion.setStartsAt(request.getStartsAt());
        promotion.setEndsAt(request.getEndsAt());

        // Mặc định ban đầu khi tạo mới sẽ cho hoạt động luôn
        promotion.setActive(true);

        promotion.setUsageCount(0);
        promotion.setCreatedAt(LocalDateTime.now());

        return promotionRepository.save(promotion);
    }

    // 3. API ĐẢO TRẠNG THÁI SWITCH
    public Promotion togglePromotionStatus(java.util.UUID id) {
        Promotion promotion = promotionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy chương trình khuyến mãi với ID: " + id));

        // Đảo ngược trạng thái hoạt động hiện tại
        promotion.setActive(!promotion.isActive());

        return promotionRepository.save(promotion);
    }

    // 4. API KÍCH HOẠT GỬI THÔNG BÁO CHIẾN DỊCH
    public void sendPromotionToTargetUsers(java.util.UUID id) {
        Promotion promotion = promotionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy chương trình với ID: " + id));

        // Lấy ra chuỗi phân khúc cần gửi (Ví dụ: "MEMBER,GOLD,PLATINUM")
        String targetTiersStr = promotion.getTargetTiers();

        // In log kiểm tra tiến trình xử lý ngầm mượt mà
        System.out.println("==========================================================================");
        System.out.println("[KÍCH HOẠT HỆ THỐNG PHÁT THÔNG BÁO CHẾ ĐỘ ADMIN]");
        System.out.println("-> Chương trình: " + promotion.getName());
        System.out.println("-> Đối tượng thụ hưởng: [" + targetTiersStr + "]");
        System.out.println("==========================================================================");
    }
}