package com.valihameed.ufcfightpredictor.repository;

import com.valihameed.ufcfightpredictor.users.UsernameHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UsernameHistoryRepository extends JpaRepository<UsernameHistory, Long> {

    Optional<UsernameHistory> findFirstByUserIdOrderByChangedAtDesc(Long userId);

    @Query("SELECT uh FROM UsernameHistory uh WHERE LOWER(uh.previousUsername) = LOWER(:username) AND uh.changedAt > :sinceDate")
    List<UsernameHistory> findRecentByPreviousUsername(@Param("username") String username, @Param("sinceDate") OffsetDateTime sinceDate);
}
