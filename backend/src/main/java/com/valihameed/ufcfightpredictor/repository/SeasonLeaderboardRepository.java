package com.valihameed.ufcfightpredictor.repository;

import com.valihameed.ufcfightpredictor.models.SeasonLeaderboard;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SeasonLeaderboardRepository extends JpaRepository<SeasonLeaderboard, Long> {
    Optional<SeasonLeaderboard> findBySeasonIdAndUserId(Long seasonId, Long userId);

    @Query("SELECT sl FROM SeasonLeaderboard sl JOIN user u ON sl.userId = u.id WHERE u.publicProfile = true AND sl.seasonId = :seasonId ORDER BY sl.totalPoints DESC")
    Page<SeasonLeaderboard> findPublicBySeasonId(@Param("seasonId") Long seasonId, Pageable pageable);

    @Query("SELECT COUNT(sl) FROM SeasonLeaderboard sl JOIN user u ON sl.userId = u.id WHERE u.publicProfile = true AND sl.seasonId = :seasonId AND sl.totalPoints > :points")
    long countUsersWithMorePointsInSeason(@Param("seasonId") Long seasonId, @Param("points") int points);
}
