package com.valihameed.ufcfightpredictor.scraper;

import com.valihameed.ufcfightpredictor.models.Event;
import com.valihameed.ufcfightpredictor.models.Fight;
import com.valihameed.ufcfightpredictor.repository.EventRepository;
import com.valihameed.ufcfightpredictor.repository.FightRepository;
import com.valihameed.ufcfightpredictor.repository.ForumThreadRepository;
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
    private final ForumThreadRepository forumThreadRepository;

    @PostMapping("/events")
    public ResponseEntity<List<Event>> upsertEvents(@RequestBody List<Event> events) {
        List<Event> savedEvents = new java.util.ArrayList<>();
        for (Event e : events) {
            java.util.Optional<Event> existing = eventRepository.findByName(e.getName());
            if (existing.isPresent()) {
                Event ev = existing.get();
                ev.setEventDate(e.getEventDate());
                ev.setLocation(e.getLocation());
                ev.setStatus(e.getStatus());
                savedEvents.add(eventRepository.save(ev));
            } else {
                Event savedEvent = eventRepository.save(e);
                savedEvents.add(savedEvent);
                // Auto-create thread for new event
                com.valihameed.ufcfightpredictor.models.ForumThread thread = com.valihameed.ufcfightpredictor.models.ForumThread.builder()
                        .eventId(savedEvent.getId())
                        .title(savedEvent.getName() + " Discussion")
                        .createdAt(java.time.OffsetDateTime.now())
                        .build();
                forumThreadRepository.save(thread);
            }
        }
        return ResponseEntity.ok().body(savedEvents);
    }

    @PostMapping("/fights")
    public ResponseEntity<List<Fight>> upsertFights(@RequestBody List<Fight> fights) {
        List<Fight> savedFights = new java.util.ArrayList<>();
        for (Fight f : fights) {
            java.util.Optional<Fight> existing = fightRepository.findByEventIdAndFighter1NameAndFighter2Name(f.getEventId(), f.getFighter1Name(), f.getFighter2Name());
            if (existing.isPresent()) {
                Fight ft = existing.get();
                ft.setWeightClass(f.getWeightClass());
                ft.setIsMainEvent(f.getIsMainEvent());
                ft.setFightOrder(f.getFightOrder());
                ft.setResultWinner(f.getResultWinner());
                ft.setResultMethod(f.getResultMethod());
                ft.setResultRound(f.getResultRound());
                ft.setResultTime(f.getResultTime());
                ft.setStatus(f.getStatus());
                savedFights.add(fightRepository.save(ft));
            } else {
                Fight savedFight = fightRepository.save(f);
                savedFights.add(savedFight);
                // Auto-create thread for new fight
                com.valihameed.ufcfightpredictor.models.ForumThread thread = com.valihameed.ufcfightpredictor.models.ForumThread.builder()
                        .eventId(savedFight.getEventId())
                        .fightId(savedFight.getId())
                        .title(savedFight.getFighter1Name() + " vs " + savedFight.getFighter2Name() + " Discussion")
                        .createdAt(java.time.OffsetDateTime.now())
                        .build();
                forumThreadRepository.save(thread);
            }
        }
        return ResponseEntity.ok().body(savedFights);
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
