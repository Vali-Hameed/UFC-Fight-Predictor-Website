package com.valihameed.ufcfightpredictor.repository;

import com.valihameed.ufcfightpredictor.models.PredictionResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PredictionResultRepository extends JpaRepository<PredictionResult, Long> {
    List<PredictionResult> findByUserPredictionId(Long userPredictionId);

    @org.springframework.data.jpa.repository.Query("SELECT up.userId, u.username, " +
            "SUM(pr.pointsAwarded), " +
            "SUM(CASE WHEN pr.isWinnerCorrect = true THEN 1 ELSE 0 END), " +
            "COUNT(pr.id) " +
            "FROM PredictionResult pr " +
            "JOIN UserPrediction up ON pr.userPredictionId = up.id " +
            "JOIN Fight f ON up.fightId = f.id " +
            "JOIN user u ON up.userId = u.id " +
            "WHERE f.eventId = :eventId " +
            "GROUP BY up.userId, u.username " +
            "ORDER BY SUM(pr.pointsAwarded) DESC")
    List<Object[]> getEventLeaderboardData(@org.springframework.data.repository.query.Param("eventId") Long eventId);
}
