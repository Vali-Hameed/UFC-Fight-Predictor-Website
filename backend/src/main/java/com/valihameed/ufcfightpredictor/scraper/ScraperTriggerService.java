package com.valihameed.ufcfightpredictor.scraper;

import com.valihameed.ufcfightpredictor.models.Event;
import com.valihameed.ufcfightpredictor.repository.EventRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.OffsetDateTime;
import java.util.List;

@Service
@Slf4j
public class ScraperTriggerService {

    private final EventRepository eventRepository;
    private final RestTemplate restTemplate;

    @Value("${scraper.url:http://localhost:8001}")
    private String scraperUrl;

    public ScraperTriggerService(EventRepository eventRepository) {
        this.eventRepository = eventRepository;
        this.restTemplate = new RestTemplate();
    }

    @Scheduled(cron = "0 0 * * * *") // Runs at the top of every hour
    public void checkAndTriggerScraper() {
        log.info("Checking if any events have finished to trigger the scraper...");
        List<Event> upcomingEvents = eventRepository.findByStatus("UPCOMING");
        
        boolean shouldTrigger = false;
        
        for (Event event : upcomingEvents) {
            if (event.getEventDate() != null) {
                // If the event started more than 6 hours ago
                if (OffsetDateTime.now().isAfter(event.getEventDate().plusHours(6))) {
                    log.info("Event {} started more than 6 hours ago. Triggering scraper.", event.getName());
                    shouldTrigger = true;
                    break;
                }
            }
        }
        
        if (shouldTrigger) {
            try {
                String url = scraperUrl + "/trigger";
                ResponseEntity<String> response = restTemplate.postForEntity(url, null, String.class);
                log.info("Scraper triggered successfully: {}", response.getBody());
            } catch (Exception e) {
                log.error("Failed to trigger scraper: {}", e.getMessage());
            }
        }
    }
}
