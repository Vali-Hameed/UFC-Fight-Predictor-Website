package com.valihameed.ufcfightpredictor.repository;

import com.valihameed.ufcfightpredictor.models.Fight;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FightRepository extends JpaRepository<Fight, Long> {
    List<Fight> findByEventIdOrderByFightOrderAsc(Long eventId);
    java.util.Optional<Fight> findByEventIdAndFighter1NameAndFighter2Name(Long eventId, String fighter1Name, String fighter2Name);

    @org.springframework.data.jpa.repository.Query("SELECT COUNT(f) FROM Fight f, MlPrediction m, Event e WHERE f.id = m.fightId AND f.eventId = e.id AND f.status = 'COMPLETED' AND f.resultWinner IS NOT NULL AND f.resultWinner = m.predictedWinner AND e.eventDate >= :since")
    long countCorrectAiPredictions(@org.springframework.data.repository.query.Param("since") java.time.OffsetDateTime since);

    @org.springframework.data.jpa.repository.Query("SELECT COUNT(f) FROM Fight f, MlPrediction m, Event e WHERE f.id = m.fightId AND f.eventId = e.id AND f.status = 'COMPLETED' AND f.resultWinner IS NOT NULL AND e.eventDate >= :since")
    long countTotalAiPredictions(@org.springframework.data.repository.query.Param("since") java.time.OffsetDateTime since);
}
