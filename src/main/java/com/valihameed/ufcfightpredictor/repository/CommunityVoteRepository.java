package com.valihameed.ufcfightpredictor.repository;

import com.valihameed.ufcfightpredictor.models.CommunityVote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CommunityVoteRepository extends JpaRepository<CommunityVote, Long> {
    Optional<CommunityVote> findByFightId(Long fightId);
}
