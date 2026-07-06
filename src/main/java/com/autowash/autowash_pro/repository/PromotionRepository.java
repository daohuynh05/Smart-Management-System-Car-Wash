package com.autowash.autowash_pro.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.autowash.autowash_pro.entity.Promotion;

public interface PromotionRepository extends JpaRepository<Promotion, UUID> {

       // 1. Tab "Tất cả": Sử dụng hàm findAll() mặc định sẵn có của JpaRepository

       // 2. Tab "Đang chạy": Lọc các chương trình được bật switch VÀ đang nằm trong
       // khung ngày hiệu lực
       @Query("SELECT p FROM Promotion p " +
                     "WHERE p.isActive = true " +
                     "AND p.startsAt <= :now " +
                     "AND p.endsAt >= :now")
       List<Promotion> findActivePromotions(@Param("now") LocalDateTime now);

       // 3. Tab "Hết hạn": Lọc các chương trình bị tắt switch HOẶC đã quá ngày kết
       // thúc so với hiện tại
       @Query("SELECT p FROM Promotion p " +
                     "WHERE p.isActive = false " +
                     "OR p.endsAt < :now")
       List<Promotion> findExpiredPromotions(@Param("now") LocalDateTime now);

       @Query("SELECT p FROM Promotion p WHERE LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%'))")
       List<Promotion> searchPromotions(@Param("keyword") String keyword);
}