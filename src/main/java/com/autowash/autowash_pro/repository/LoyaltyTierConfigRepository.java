package com.autowash.autowash_pro.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.autowash.autowash_pro.entity.LoyaltyTierConfig;

public interface LoyaltyTierConfigRepository extends JpaRepository<LoyaltyTierConfig, UUID> {
    Optional<LoyaltyTierConfig> findByActiveTrue();
}
