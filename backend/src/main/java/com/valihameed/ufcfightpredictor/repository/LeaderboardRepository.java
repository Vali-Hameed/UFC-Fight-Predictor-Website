package com.valihameed.ufcfightpredictor.repository;

import com.valihameed.ufcfightpredictor.models.Leaderboard;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface LeaderboardRepository extends JpaRepository<Leaderboard, Long> {
    Optional<Leaderboard> findByUserId(Long userId);

    @Query("SELECT l FROM Leaderboard l JOIN user u ON l.userId = u.id WHERE u.publicProfile = true ORDER BY l.totalPoints DESC")
    Page<Leaderboard> findPublicLeaderboard(Pageable pageable);

    @Query("SELECT COUNT(l) FROM Leaderboard l JOIN user u ON l.userId = u.id WHERE u.publicProfile = true AND l.totalPoints > :points")
    long countUsersWithMorePoints(@Param("points") int points);
}
