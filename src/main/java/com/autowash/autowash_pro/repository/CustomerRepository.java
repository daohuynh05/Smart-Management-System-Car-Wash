package com.autowash.autowash_pro.repository;

import com.autowash.autowash_pro.entity.Customer;
import com.autowash.autowash_pro.enums.Tier;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CustomerRepository extends JpaRepository<Customer, UUID> {

    boolean existsByEmail(String email);

    boolean existsByPhone(String phone);

    Optional<Customer> findByPhone(String phone);

    Optional<Customer> findByEmail(String email);

    // Giữ nguyên hàm lọc theo hạng thành viên của bạn
    List<Customer> findByTier(Tier tier);

    // Tìm kiếm thông minh theo Tên hoặc Số điện thoại phục vụ ô Search Admin
    @Query("SELECT c FROM Customer c " +
            "WHERE LOWER(c.fullName) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
            "OR c.phone LIKE CONCAT('%', :keyword, '%')")
    List<Customer> searchCustomers(@Param("keyword") String keyword);

    List<Customer> findByRegisteredAtBetween(java.time.LocalDateTime start, java.time.LocalDateTime end);
}