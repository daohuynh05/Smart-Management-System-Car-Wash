package com.autowash.autowash_pro.repository;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import com.autowash.autowash_pro.entity.WashService;

public interface WashServiceRepository extends JpaRepository<WashService, UUID> {
    List<WashService> findByIsActiveTrue();
    boolean existsByName(String name);
}
