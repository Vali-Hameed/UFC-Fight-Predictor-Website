package com.valihameed.ufcfightpredictor.scraper;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.valihameed.ufcfightpredictor.repository.FightRepository;
import com.valihameed.ufcfightpredictor.models.Event;
import com.valihameed.ufcfightpredictor.models.Fight;
import com.valihameed.ufcfightpredictor.repository.EventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.OffsetDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class EspnLiveScraperService {

    private final EventRepository eventRepository;
    private final FightRepository fightRepository;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final com.valihameed.ufcfightpredictor.results.ResultProcessingService resultProcessingService;

    private static final String ESPN_SCOREBOARD_URL = "https://site.api.espn.com/apis/site/v2/sports/mma/ufc/scoreboard";

    @Scheduled(fixedRate = 30000) // Every 30 seconds
    public void pollLiveEvents() {
        try {
            List<Event> upcomingEvents = eventRepository.findByStatus("UPCOMING");
            Event liveEvent = null;

            OffsetDateTime now = OffsetDateTime.now();
            for (Event event : upcomingEvents) {
                if (event.getEventDate() != null) {
                    OffsetDateTime eventTime = event.getEventDate();
                    // If event has started (time is past)
                    if (now.isAfter(eventTime)) {
                        liveEvent = event;
                        break;
                    }
                }
            }

            if (liveEvent == null) {
                // Also do a quick cleanup of orphaned UPCOMING fights for events older than 24 hours
                List<Fight> orphanedFights = fightRepository.findByStatus("UPCOMING");
                for (Fight f : orphanedFights) {
                    eventRepository.findById(f.getEventId()).ifPresent(event -> {
                        if (event.getEventDate() != null && now.isAfter(event.getEventDate().plusHours(24))) {
                            log.info("Fight {} vs {} is still UPCOMING 24h after event. Auto-cancelling.", f.getFighter1Name(), f.getFighter2Name());
                            f.setStatus("CANCELED");
                            f.setResultWinner("Canceled");
                            f.setResultMethod("Canceled");
                            fightRepository.save(f);
                            try {
                                resultProcessingService.processFightResult(f.getId());
                            } catch (Exception ex) {
                                log.error("Failed to process auto-cancelled fight {}: {}", f.getId(), ex.getMessage());
                            }
                        }
                    });
                }
                return; // No live event to poll
            }

            log.info("Polling ESPN for live event: {}", liveEvent.getName());
            String response = restTemplate.getForObject(ESPN_SCOREBOARD_URL, String.class);
            if (response == null) return;

            JsonNode root = objectMapper.readTree(response);
            JsonNode eventsNode = root.path("events");

            List<Fight> dbFights = fightRepository.findByEventIdOrderByFightOrderAsc(liveEvent.getId());
            boolean anyFightUpdated = false;

            for (JsonNode espnEvent : eventsNode) {
                JsonNode competitions = espnEvent.path("competitions");
                for (JsonNode comp : competitions) {
                    JsonNode competitors = comp.path("competitors");
                    if (competitors.size() != 2) continue;

                    String f1Name = competitors.get(0).path("athlete").path("fullName").asText();
                    String f2Name = competitors.get(1).path("athlete").path("fullName").asText();

                    // Find matching fight in DB
                    Fight matchedFight = ScraperUtils.fuzzyMatchFight(dbFights, f1Name, f2Name);
                    if (matchedFight != null) {
                        JsonNode statusNode = comp.path("status");
                        double clock = statusNode.path("clock").asDouble(0.0);
                        String displayClock = statusNode.path("displayClock").asText("");
                        int period = statusNode.path("period").asInt(0);
                        JsonNode typeNode = statusNode.path("type");
                        boolean completed = typeNode.path("completed").asBoolean(false);
                        String state = typeNode.path("state").asText("");
                        String statusName = typeNode.path("name").asText("");

                        matchedFight.setCurrentRound(period);
                        matchedFight.setCurrentClock(displayClock);
                        matchedFight.setLiveStatus(statusName);

                        boolean isCanceledStatus = statusName != null && statusName.toLowerCase().contains("canceled");
                        
                        if (isCanceledStatus && !"CANCELED".equals(matchedFight.getStatus())) {
                            log.info("Fight canceled on ESPN: {} vs {}", f1Name, f2Name);
                            matchedFight.setStatus("CANCELED");
                            matchedFight.setResultWinner("Canceled");
                            matchedFight.setResultMethod("Canceled");
                            fightRepository.save(matchedFight);

                            try {
                                resultProcessingService.processFightResult(matchedFight.getId());
                            } catch (Exception ex) {
                                log.error("Failed to process results for canceled fight {}: {}", matchedFight.getId(), ex.getMessage());
                            }
                            anyFightUpdated = true;
                            continue;
                        }

                        if (completed && !"COMPLETED".equals(matchedFight.getStatus()) && !"CANCELED".equals(matchedFight.getStatus())) {
                            // Fight just finished (or got stuck previously), update official results!
                            log.info("Fight completed on ESPN: {} vs {}", f1Name, f2Name);
                            matchedFight.setResultRound(period);
                            matchedFight.setResultTime(displayClock);
                            
                            // Determine winner correctly by mapping ESPN names to our DB names
                            boolean f1Winner = competitors.get(0).path("winner").asBoolean(false);
                            boolean f2Winner = competitors.get(1).path("winner").asBoolean(false);

                            String winningEspnName = null;
                            if (f1Winner) winningEspnName = f1Name;
                            else if (f2Winner) winningEspnName = f2Name;

                            // Try to get method (avoid 'Final' string)
                            String detail = typeNode.path("detail").asText("");
                            if (detail.equalsIgnoreCase("Final") || detail.contains("STATUS_")) {
                                detail = ""; // Blank out 'Final' so frontend falls back cleanly or avoids it
                            }

                            // Try to find the exact method in ESPN's live 'details' array
                            JsonNode detailsArray = comp.path("details");
                            if (detailsArray.isArray()) {
                                for (JsonNode dObj : detailsArray) {
                                    String text = dObj.path("type").path("text").asText("").toLowerCase();
                                    if (text.contains("kotko")) {
                                        detail = "KO/TKO";
                                        break;
                                    } else if (text.contains("sub") && text.contains("winner")) {
                                        detail = "Submission";
                                        break;
                                    } else if (text.contains("dec") && text.contains("winner")) {
                                        detail = "Decision";
                                        break;
                                    } else if (text.contains("draw")) {
                                        detail = "Draw";
                                        break;
                                    } else if (text.contains("no contest")) {
                                        detail = "No Contest";
                                        break;
                                    }
                                }
                            }

                            if (winningEspnName != null || "Draw".equalsIgnoreCase(detail) || "No Contest".equalsIgnoreCase(detail)) {
                                if (winningEspnName != null) {
                                    if (ScraperUtils.isMatch(matchedFight.getFighter1Name(), winningEspnName)) {
                                        matchedFight.setResultWinner(matchedFight.getFighter1Name());
                                    } else if (ScraperUtils.isMatch(matchedFight.getFighter2Name(), winningEspnName)) {
                                        matchedFight.setResultWinner(matchedFight.getFighter2Name());
                                    } else {
                                        matchedFight.setResultWinner(winningEspnName); // fallback
                                    }
                                } else if ("No Contest".equalsIgnoreCase(detail)) {
                                    matchedFight.setResultWinner("No Contest");
                                } else {
                                    matchedFight.setResultWinner("Draw/NC"); // Legacy fallback for true draw
                                }
                                
                                matchedFight.setResultMethod(detail);
                                matchedFight.setStatus("COMPLETED");

                                // Save the fight immediately so the processing service can read it
                                fightRepository.save(matchedFight);

                                // Trigger the point calculation and notifications!
                                try {
                                    resultProcessingService.processFightResult(matchedFight.getId());
                                } catch (Exception ex) {
                                    log.error("Failed to process results for fight {}: {}", matchedFight.getId(), ex.getMessage());
                                }
                            } else {
                                log.info("ESPN marked completed but no winner yet. Waiting for update.");
                            }
                        }

                        anyFightUpdated = true;
                    }
                }
            }

            if (anyFightUpdated) {
                fightRepository.saveAll(dbFights);
            }

        } catch (Exception e) {
            log.error("Error polling ESPN live events: {}", e.getMessage());
        }
    }
}
