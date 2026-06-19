package com.valihameed.ufcfightpredictor.repository;

import com.valihameed.ufcfightpredictor.models.EventLeaderboard;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface EventLeaderboardRepository extends JpaRepository<EventLeaderboard, Long> {
    Optional<EventLeaderboard> findByEventIdAndUserId(Long eventId, Long userId);

    @Query("SELECT el FROM EventLeaderboard el JOIN user u ON el.userId = u.id WHERE u.publicProfile = true AND el.eventId = :eventId ORDER BY el.totalPoints DESC")
    Page<EventLeaderboard> findPublicByEventId(@Param("eventId") Long eventId, Pageable pageable);
}
