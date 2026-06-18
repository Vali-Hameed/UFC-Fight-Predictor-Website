package com.valihameed.ufcfightpredictor.events;

import com.valihameed.ufcfightpredictor.models.Event;
import com.valihameed.ufcfightpredictor.repository.EventRepository;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

@Service
@Slf4j
@AllArgsConstructor
public class EventArchiverService {

    private final EventRepository eventRepository;

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void runOnStartup() {
        log.info("Running event archiver on startup...");
        archiveOldEvents();
    }

    @Scheduled(cron = "0 0 * * * *") // Run every hour at the top of the hour
    @Transactional
    public void runSchedule() {
        log.info("Running scheduled event archiver...");
        archiveOldEvents();
    }

    private void archiveOldEvents() {
        OffsetDateTime now = OffsetDateTime.now();
        // Fetch all events that have occurred in the past, ordered by newest first
        List<Event> pastEvents = eventRepository.findAllByEventDateBeforeOrderByEventDateDesc(now, PageRequest.of(0, 1000)).getContent();

        if (pastEvents.size() <= 1) {
            log.info("Not enough past events to archive. Total past events: {}", pastEvents.size());
            return;
        }

        // The first event is the most recent past event. It remains in the active schedule.
        // We iterate through all subsequent events and mark them as ARCHIVED if they aren't already.
        int archivedCount = 0;
        for (int i = 1; i < pastEvents.size(); i++) {
            Event event = pastEvents.get(i);
            if (!"ARCHIVED".equals(event.getStatus())) {
                event.setStatus("ARCHIVED");
                eventRepository.save(event);
                archivedCount++;
                log.info("Archived event: {} (ID: {})", event.getName(), event.getId());
            }
        }

        if (archivedCount > 0) {
            log.info("Successfully archived {} old events.", archivedCount);
        } else {
            log.info("No old events needed archiving.");
        }
    }
}
