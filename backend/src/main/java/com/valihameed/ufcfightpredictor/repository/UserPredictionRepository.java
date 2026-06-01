package com.valihameed.ufcfightpredictor.repository;

import com.valihameed.ufcfightpredictor.models.UserPrediction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserPredictionRepository extends JpaRepository<UserPrediction, Long> {
    List<UserPrediction> findByUserId(Long userId);
    List<UserPrediction> findByFightId(Long fightId);
    Optional<UserPrediction> findFirstByUserIdAndFightId(Long userId, Long fightId);
}
