package com.valihameed.ufcfightpredictor.repository;

import com.valihameed.ufcfightpredictor.models.PredictionResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PredictionResultRepository extends JpaRepository<PredictionResult, Long> {
    List<PredictionResult> findByUserPredictionId(Long userPredictionId);
}
