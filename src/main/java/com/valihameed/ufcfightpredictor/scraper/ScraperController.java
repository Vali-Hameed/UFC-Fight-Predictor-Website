package com.valihameed.ufcfightpredictor.scraper;

import com.valihameed.ufcfightpredictor.models.Event;
import com.valihameed.ufcfightpredictor.models.Fight;
import com.valihameed.ufcfightpredictor.repository.EventRepository;
import com.valihameed.ufcfightpredictor.repository.FightRepository;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/internal/scraper")
@AllArgsConstructor
public class ScraperController {
    private final EventRepository eventRepository;
    private final FightRepository fightRepository;

    @PostMapping("/events")
    public ResponseEntity<?> upsertEvents(@RequestBody List<Event> events) {
        // upsert by some unique combination; for simplicity saveAll (caller should avoid duplicates)
        eventRepository.saveAll(events);
        return ResponseEntity.ok().body("events upserted");
    }

    @PostMapping("/fights")
    public ResponseEntity<?> upsertFights(@RequestBody List<Fight> fights) {
        fightRepository.saveAll(fights);
        return ResponseEntity.ok().body("fights upserted");
    }

    @PostMapping("/results")
    public ResponseEntity<?> upsertResults(@RequestBody List<Fight> fights) {
        for (Fight f : fights) {
            if (f.getId() == null) continue;
            fightRepository.findById(f.getId()).ifPresent(existing -> {
                existing.setResultWinner(f.getResultWinner());
                existing.setResultMethod(f.getResultMethod());
                existing.setResultRound(f.getResultRound());
                existing.setResultTime(f.getResultTime());
                existing.setStatus(f.getStatus());
                fightRepository.save(existing);
            });
        }
        return ResponseEntity.ok().body("results upserted");
    }
}
