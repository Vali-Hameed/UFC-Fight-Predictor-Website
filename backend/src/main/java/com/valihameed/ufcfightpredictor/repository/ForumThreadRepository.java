package com.valihameed.ufcfightpredictor.repository;

import com.valihameed.ufcfightpredictor.models.ForumThread;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ForumThreadRepository extends JpaRepository<ForumThread, Long> {
    List<ForumThread> findByEventId(Long eventId);
    List<ForumThread> findByFightId(Long fightId);
    boolean existsByEventIdAndFightIdIsNull(Long eventId);
    boolean existsByFightId(Long fightId);
}
