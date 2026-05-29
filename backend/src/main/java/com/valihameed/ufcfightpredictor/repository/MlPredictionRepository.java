package com.valihameed.ufcfightpredictor.repository;

import com.valihameed.ufcfightpredictor.models.MlPrediction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MlPredictionRepository extends JpaRepository<MlPrediction, Long> {
    Optional<MlPrediction> findByFightId(Long fightId);
}
