package com.valihameed.ufcfightpredictor.scraper;

import com.valihameed.ufcfightpredictor.models.Event;
import com.valihameed.ufcfightpredictor.models.Fight;
import com.valihameed.ufcfightpredictor.repository.EventRepository;
import com.valihameed.ufcfightpredictor.repository.FightRepository;
import com.valihameed.ufcfightpredictor.repository.ForumThreadRepository;
import com.valihameed.ufcfightpredictor.repository.UserPredictionRepository;
import com.valihameed.ufcfightpredictor.repository.NotificationRepository;
import com.valihameed.ufcfightpredictor.notifications.NotificationService;
import com.valihameed.ufcfightpredictor.repository.userRepository;
import com.valihameed.ufcfightpredictor.models.Notification;
import com.valihameed.ufcfightpredictor.models.UserPrediction;
import com.valihameed.ufcfightpredictor.users.user;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import com.valihameed.ufcfightpredictor.results.ResultProcessingService;

@RestController
@RequestMapping("/api/v1/internal/scraper")
@AllArgsConstructor
public class ScraperController {
    private final EventRepository eventRepository;
    private final FightRepository fightRepository;
    private final ForumThreadRepository forumThreadRepository;
    private final UserPredictionRepository userPredictionRepository;
    private final NotificationRepository notificationRepository;
    private final NotificationService notificationService;
    private final userRepository userRepository;
    private final ResultProcessingService resultProcessingService;

    @PostMapping("/events")
    public ResponseEntity<List<Event>> upsertEvents(@RequestBody List<Event> events) {
        List<Event> savedEvents = new java.util.ArrayList<>();
        for (Event e : events) {
            java.util.Optional<Event> existing = java.util.Optional.empty();
            List<Event> upcomingEvents = eventRepository.findByStatus("UPCOMING");
            
            // 1. Try exact name match + within 48 hours
            for (Event upc : upcomingEvents) {
                if (e.getName().equals(upc.getName()) && e.getEventDate() != null && upc.getEventDate() != null) {
                    long hoursDiff = Math.abs(java.time.Duration.between(e.getEventDate(), upc.getEventDate()).toHours());
                    if (hoursDiff <= 48) {
                        existing = java.util.Optional.of(upc);
                        break;
                    }
                }
            }

            // 2. Try fuzzy name match (e.g. "UFC Fight Night" -> "UFC Fight Night: A vs B") + within 48 hours
            if (existing.isEmpty()) {
                for (Event upc : upcomingEvents) {
                    if ((e.getName().startsWith(upc.getName()) || upc.getName().startsWith(e.getName())) 
                        && e.getEventDate() != null && upc.getEventDate() != null) {
                        long hoursDiff = Math.abs(java.time.Duration.between(e.getEventDate(), upc.getEventDate()).toHours());
                        if (hoursDiff <= 48) {
                            existing = java.util.Optional.of(upc);
                            if (e.getName().length() > upc.getName().length()) {
                                upc.setName(e.getName()); // Update to the more specific name
                            }
                            break;
                        }
                    }
                }
            }

            // 3. Try exact date match (for events that were renamed completely)
            if (existing.isEmpty()) {
                for (Event upc : upcomingEvents) {
                    if (e.getEventDate() != null && upc.getEventDate() != null && e.getEventDate().isEqual(upc.getEventDate())) {
                        existing = java.util.Optional.of(upc);
                        upc.setName(e.getName()); // Update to the new name
                        break;
                    }
                }
            }

            // 4. Fallback to global find by name (for COMPLETED events or matching without date)
            if (existing.isEmpty()) {
                try {
                    existing = eventRepository.findByName(e.getName());
                } catch (Exception ex) {
                    // Ignore NonUniqueResultException or others and just proceed
                }
            }

            if (existing.isPresent()) {
                Event ev = existing.get();
                ev.setEventDate(e.getEventDate());
                ev.setLocation(e.getLocation());
                ev.setStatus(e.getStatus());
                savedEvents.add(eventRepository.save(ev));
                
                // Auto-create thread if missing
                if (!forumThreadRepository.existsByEventIdAndFightIdIsNull(ev.getId())) {
                    com.valihameed.ufcfightpredictor.models.ForumThread thread = com.valihameed.ufcfightpredictor.models.ForumThread.builder()
                            .eventId(ev.getId())
                            .title(ev.getName() + " Discussion")
                            .createdAt(java.time.OffsetDateTime.now())
                            .build();
                    forumThreadRepository.save(thread);
                }
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
        java.util.Set<Long> eventIds = new java.util.HashSet<>();
        java.util.Set<Long> processedFightIds = new java.util.HashSet<>();
        
        java.util.Map<Long, List<Fight>> dbFightsByEvent = new java.util.HashMap<>();
        
        List<Fight> savedFights = new java.util.ArrayList<>();
        for (Fight f : fights) {
            if (f.getEventId() != null) {
                eventIds.add(f.getEventId());
                if (!dbFightsByEvent.containsKey(f.getEventId())) {
                    dbFightsByEvent.put(f.getEventId(), fightRepository.findByEventIdOrderByFightOrderAsc(f.getEventId()));
                }
            }
            
            Fight matchedFight = null;
            if (f.getEventId() != null) {
                matchedFight = ScraperUtils.fuzzyMatchFight(dbFightsByEvent.get(f.getEventId()), f.getFighter1Name(), f.getFighter2Name());
            }
            
            if (matchedFight != null) {
                Fight ft = matchedFight;
                String oldStatus = ft.getStatus();
                String oldWinner = ft.getResultWinner();

                ft.setFighter1Name(f.getFighter1Name());
                ft.setFighter2Name(f.getFighter2Name());

                ft.setWeightClass(f.getWeightClass());
                ft.setIsMainEvent(f.getIsMainEvent());
                ft.setFightOrder(f.getFightOrder());
                ft.setResultWinner(f.getResultWinner());
                ft.setResultMethod(f.getResultMethod());
                ft.setResultRound(f.getResultRound());
                ft.setResultTime(f.getResultTime());
                ft.setStatus(f.getStatus());
                Fight savedFight = fightRepository.save(ft);
                savedFights.add(savedFight);
                processedFightIds.add(savedFight.getId());
                
                boolean isNewlyCompleted = ("COMPLETED".equals(savedFight.getStatus()) || "CANCELED".equals(savedFight.getStatus())) &&
                        (!"COMPLETED".equals(oldStatus) && !"CANCELED".equals(oldStatus));
                        
                boolean resultChanged = savedFight.getResultWinner() != null && !savedFight.getResultWinner().equals(oldWinner);

                if ((isNewlyCompleted || resultChanged) && savedFight.getResultWinner() != null) {
                    try {
                        resultProcessingService.processFightResult(savedFight.getId());
                    } catch (Exception ex) {
                        // Ignore any errors in processing so the scraper doesn't fail
                    }
                }
                
                // Auto-create thread if missing
                if (!forumThreadRepository.existsByFightId(ft.getId())) {
                    com.valihameed.ufcfightpredictor.models.ForumThread thread = com.valihameed.ufcfightpredictor.models.ForumThread.builder()
                            .eventId(ft.getEventId())
                            .fightId(ft.getId())
                            .title(ft.getFighter1Name() + " vs " + ft.getFighter2Name() + " Discussion")
                            .createdAt(java.time.OffsetDateTime.now())
                            .build();
                    forumThreadRepository.save(thread);
                }
            } else {
                Fight savedFight = fightRepository.save(f);
                savedFights.add(savedFight);
                processedFightIds.add(savedFight.getId());
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

        // Mark missing fights as CANCELED
        for (Long eventId : eventIds) {
            List<Fight> existingFightsForEvent = fightRepository.findByEventIdOrderByFightOrderAsc(eventId);
            for (Fight dbFight : existingFightsForEvent) {
                if (!processedFightIds.contains(dbFight.getId()) && !"CANCELED".equals(dbFight.getStatus()) && !"COMPLETED".equals(dbFight.getStatus())) {
                    dbFight.setStatus("CANCELED");
                    fightRepository.save(dbFight);
                }
            }
        }
        
        return ResponseEntity.ok().body(savedFights);
    }

    @PostMapping("/results")
    public ResponseEntity<?> upsertResults(@RequestBody List<Fight> fights) {
        for (Fight f : fights) {
            if (f.getId() == null) continue;
            fightRepository.findById(f.getId()).ifPresent(existing -> {
                boolean isNewResult = existing.getResultWinner() == null && f.getResultWinner() != null;
                
                existing.setResultWinner(f.getResultWinner());
                existing.setResultMethod(f.getResultMethod());
                existing.setResultRound(f.getResultRound());
                existing.setResultTime(f.getResultTime());
                existing.setStatus(f.getStatus());
                fightRepository.save(existing);
                
                if (isNewResult) {
                    List<UserPrediction> predictions = userPredictionRepository.findByFightId(existing.getId());
                    for (UserPrediction pred : predictions) {
                        if (pred.getOptOutResultNotification() != null && pred.getOptOutResultNotification()) continue;
                        userRepository.findById(pred.getUserId()).ifPresent(u -> {
                            Notification n = Notification.builder()
                                    .userId(u.getId())
                                    .type("FIGHT_RESULT")
                                    .message("Results are out for " + existing.getFighter1Name() + " vs " + existing.getFighter2Name() + "!")
                                    .link("/events")
                                    .build();
                                notificationService.createNotification(n);
                        });
                    }
                }
            });
        }
        return ResponseEntity.ok().body("results upserted");
    }
    @PostMapping("/roster")
    public ResponseEntity<?> upsertRoster(@RequestBody java.util.Map<String, Object> roster) {
        try {
            java.io.File directory = new java.io.File("data");
            if (!directory.exists()) {
                directory.mkdir();
            }
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            mapper.writerWithDefaultPrettyPrinter().writeValue(new java.io.File("data/fighters.json"), roster);
            return ResponseEntity.ok().body("roster upserted");
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error saving roster: " + e.getMessage());
        }
    }
    
    @PostMapping("/events/cleanup-duplicates")
    @org.springframework.security.access.prepost.PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<?> cleanupDuplicates() {
        List<Fight> allFights = fightRepository.findAll();
        java.util.Map<Long, List<Fight>> fightsByEvent = new java.util.HashMap<>();
        for (Fight f : allFights) {
            if (f.getEventId() != null) {
                fightsByEvent.computeIfAbsent(f.getEventId(), k -> new java.util.ArrayList<>()).add(f);
            }
        }

        int deletedCount = 0;
        for (List<Fight> eventFights : fightsByEvent.values()) {
            List<Fight> canceledFights = new java.util.ArrayList<>();
            List<Fight> activeFights = new java.util.ArrayList<>();
            for (Fight f : eventFights) {
                if ("CANCELED".equals(f.getStatus())) {
                    canceledFights.add(f);
                } else {
                    activeFights.add(f);
                }
            }

            for (Fight canceled : canceledFights) {
                // Check if this canceled fight fuzzily matches any active fight
                Fight matched = ScraperUtils.fuzzyMatchFight(activeFights, canceled.getFighter1Name(), canceled.getFighter2Name());
                if (matched != null) {
                    // It's a duplicate, delete the canceled one
                    fightRepository.delete(canceled);
                    deletedCount++;
                }
            }
        }
        
        return ResponseEntity.ok().body("Deleted " + deletedCount + " duplicate canceled fights.");
    }
}
