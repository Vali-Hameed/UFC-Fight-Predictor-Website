package com.valihameed.ufcfightpredictor.repository;

import com.valihameed.ufcfightpredictor.models.CommunityVote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import jakarta.persistence.LockModeType;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CommunityVoteRepository extends JpaRepository<CommunityVote, Long> {
    Optional<CommunityVote> findByFightId(Long fightId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("SELECT c FROM CommunityVote c WHERE c.fightId = :fightId")
    Optional<CommunityVote> findLockedByFightId(@org.springframework.data.repository.query.Param("fightId") Long fightId);
}
