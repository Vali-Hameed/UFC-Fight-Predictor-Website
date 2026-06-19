package com.valihameed.ufcfightpredictor.repository;

import com.valihameed.ufcfightpredictor.models.UserBadge;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserBadgeRepository extends JpaRepository<UserBadge, Long> {
    List<UserBadge> findByUserId(Long userId);

    List<UserBadge> findByUserIdAndBadgeType(Long userId, String badgeType);

    boolean existsByUserIdAndBadgeTypeAndEventId(Long userId, String badgeType, Long eventId);

    boolean existsByUserIdAndBadgeTypeAndSeasonId(Long userId, String badgeType, Long seasonId);

    long countByUserIdAndBadgeType(Long userId, String badgeType);
}
