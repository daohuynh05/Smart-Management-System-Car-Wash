package com.autowash.autowash_pro.service;

import com.autowash.autowash_pro.entity.Reward;
import com.autowash.autowash_pro.entity.SystemConfig;
import com.autowash.autowash_pro.entity.TierRule;
import com.autowash.autowash_pro.repository.RewardRepository;
import com.autowash.autowash_pro.repository.SystemConfigRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class AdminConfigService {

    private final SystemConfigRepository systemConfigRepository;
    private final RewardRepository rewardRepository;

    public SystemConfig getSystemConfig() {
        List<SystemConfig> configs = systemConfigRepository.findAll();
        if (configs.isEmpty()) {
            return initializeDefaultConfig();
        }
        return configs.get(0);
    }

    public SystemConfig updateSystemConfig(SystemConfig newConfig) {
        List<SystemConfig> configs = systemConfigRepository.findAll();
        SystemConfig existing;
        if (configs.isEmpty()) {
            existing = new SystemConfig();
        } else {
            existing = configs.get(0);
        }

        existing.setPointRate(newConfig.getPointRate());

        // Cập nhật Tier Rules một cách an toàn
        List<TierRule> existingRules = existing.getTierRules();
        if (existingRules == null) {
            existingRules = new ArrayList<>();
            existing.setTierRules(existingRules);
        }

        if (newConfig.getTierRules() != null) {
            List<TierRule> updatedRules = new ArrayList<>();
            for (TierRule incomingRule : newConfig.getTierRules()) {
                if (incomingRule.getLabel() == null) {
                    incomingRule.setLabel(incomingRule.getTier().toUpperCase());
                }
                if (incomingRule.getClassName() == null) {
                    incomingRule.setClassName("bg-tier-" + incomingRule.getTier().toLowerCase());
                }

                TierRule matchedRule = existingRules.stream()
                        .filter(r -> r.getTier().equalsIgnoreCase(incomingRule.getTier()))
                        .findFirst()
                        .orElse(null);

                if (matchedRule != null) {
                    matchedRule.setName(incomingRule.getName());
                    matchedRule.setThreshold(incomingRule.getThreshold());
                    matchedRule.setBookingWindow(incomingRule.getBookingWindow());
                    matchedRule.setMultiplier(incomingRule.getMultiplier());
                    matchedRule.setPerks(incomingRule.getPerks());
                    matchedRule.setLabel(incomingRule.getLabel());
                    matchedRule.setClassName(incomingRule.getClassName());
                    updatedRules.add(matchedRule);
                } else {
                    incomingRule.setId(null);
                    updatedRules.add(incomingRule);
                }
            }
            existingRules.clear();
            existingRules.addAll(updatedRules);
        }

        return systemConfigRepository.save(existing);
    }

    public Reward addReward(Reward reward) {
        if (reward.getTierClassName() == null && reward.getMinimumTier() != null) {
            reward.setTierClassName("tier-" + reward.getMinimumTier().toLowerCase());
        }
        return rewardRepository.save(reward);
    }

    public void deleteReward(Long id) {
        rewardRepository.deleteById(id);
    }

    private SystemConfig initializeDefaultConfig() {
        List<TierRule> rules = new ArrayList<>();
        rules.add(TierRule.builder()
                .tier("member")
                .label("MEMBER")
                .name("Cấu hình mặc định")
                .threshold(0)
                .bookingWindow(3)
                .multiplier(100)
                .perks("Thành viên mới đăng ký. Tích điểm cơ bản cho mỗi dịch vụ.")
                .className("tier-member")
                .build());
        rules.add(TierRule.builder()
                .tier("silver")
                .label("SILVER")
                .name("Hạng Bạc")
                .threshold(5)
                .bookingWindow(7)
                .multiplier(110)
                .perks("Giảm giá 5% cho các dịch vụ rửa xe cao cấp. Ưu tiên đặt lịch trước 7 ngày.")
                .className("tier-silver")
                .build());
        rules.add(TierRule.builder()
                .tier("gold")
                .label("GOLD")
                .name("Hạng Vàng")
                .threshold(15)
                .bookingWindow(14)
                .multiplier(125)
                .perks("Miễn phí dịch vụ hút bụi. Giảm giá 10% các gói Detail. Ưu tiên hàng chờ cao.")
                .className("tier-gold")
                .build());
        rules.add(TierRule.builder()
                .tier("platinum")
                .label("PLATINUM")
                .name("Hạng Bạch Kim")
                .threshold(30)
                .bookingWindow(30)
                .multiplier(150)
                .perks("Chăm sóc đặc biệt. Miễn phí nâng cấp gói rửa. Quà tặng sinh nhật trị giá 500k.")
                .className("tier-platinum")
                .build());

        SystemConfig config = SystemConfig.builder()
                .pointRate("10.000")
                .tierRules(rules)
                .build();

        return systemConfigRepository.save(config);
    }

}
