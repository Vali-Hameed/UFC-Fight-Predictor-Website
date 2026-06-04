package com.valihameed.ufcfightpredictor.forum;

import com.valihameed.ufcfightpredictor.models.Event;
import com.valihameed.ufcfightpredictor.models.Fight;
import com.valihameed.ufcfightpredictor.models.ForumThread;
import com.valihameed.ufcfightpredictor.repository.EventRepository;
import com.valihameed.ufcfightpredictor.repository.FightRepository;
import com.valihameed.ufcfightpredictor.repository.ForumThreadRepository;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

@Component
@AllArgsConstructor
public class ForumThreadAutoGenerator {
    private static final Logger log = LoggerFactory.getLogger(ForumThreadAutoGenerator.class);

    private final EventRepository eventRepository;
    private final FightRepository fightRepository;
    private final ForumThreadRepository forumThreadRepository;

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void generateMissingThreadsOnStartup() {
        log.info("Checking for missing forum threads on startup...");
        int createdCount = 0;

        List<Event> events = eventRepository.findAll();
        for (Event event : events) {
            if (!forumThreadRepository.existsByEventIdAndFightIdIsNull(event.getId())) {
                ForumThread thread = ForumThread.builder()
                        .eventId(event.getId())
                        .title(event.getName() + " Discussion")
                        .createdAt(OffsetDateTime.now())
                        .build();
                forumThreadRepository.save(thread);
                createdCount++;
            }
        }

        List<Fight> fights = fightRepository.findAll();
        for (Fight fight : fights) {
            if (fight.getFighter1Name() == null || fight.getFighter2Name() == null) continue;
            
            if (!forumThreadRepository.existsByFightId(fight.getId())) {
                ForumThread thread = ForumThread.builder()
                        .eventId(fight.getEventId())
                        .fightId(fight.getId())
                        .title(fight.getFighter1Name() + " vs " + fight.getFighter2Name() + " Discussion")
                        .createdAt(OffsetDateTime.now())
                        .build();
                forumThreadRepository.save(thread);
                createdCount++;
            }
        }

        log.info("Auto-generated {} missing forum threads.", createdCount);
    }
}
