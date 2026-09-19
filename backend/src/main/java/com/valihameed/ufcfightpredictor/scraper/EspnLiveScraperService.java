package com.valihameed.ufcfightpredictor.scraper;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.valihameed.ufcfightpredictor.repository.FightRepository;
import com.valihameed.ufcfightpredictor.models.Event;
import com.valihameed.ufcfightpredictor.models.Fight;
import com.valihameed.ufcfightpredictor.repository.EventRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;

@Slf4j
@Service
public class EspnLiveScraperService {

    private final EventRepository eventRepository;
    private final FightRepository fightRepository;
    private final RestTemplate espnRestTemplate;
    private final ObjectMapper objectMapper;
    private final com.valihameed.ufcfightpredictor.results.ResultProcessingService resultProcessingService;

    // --- Resilience state ---
    private int consecutiveFailures = 0;
    private long nextAllowedPollEpochMs = 0;       // backoff: earliest time we may poll
    private long circuitBreakerOpenUntilMs = 0;     // circuit breaker cooldown end
    private long lastNonLivePollEpochMs = 0;        // throttle non-live-event polling

    private static final String ESPN_SCOREBOARD_URL = "https://site.api.espn.com/apis/site/v2/sports/mma/ufc/scoreboard";

    // --- Tuning constants ---
    private static final int MAX_BACKOFF_SECONDS = 900;            // 15 minutes
    private static final int CIRCUIT_BREAKER_THRESHOLD = 10;       // consecutive failures before opening
    private static final long CIRCUIT_BREAKER_COOLDOWN_MS = 30 * 60 * 1000L; // 30 minutes
    private static final long NON_LIVE_POLL_INTERVAL_MS = 10 * 60 * 1000L;   // 10 minutes
    private static final int LIVE_EVENT_WINDOW_HOURS = 10;         // max hours after event start to consider it live

    public EspnLiveScraperService(
            EventRepository eventRepository,
            FightRepository fightRepository,
            @Qualifier("espnRestTemplate") RestTemplate espnRestTemplate,
            ObjectMapper objectMapper,
            com.valihameed.ufcfightpredictor.results.ResultProcessingService resultProcessingService) {
        this.eventRepository = eventRepository;
        this.fightRepository = fightRepository;
        this.espnRestTemplate = espnRestTemplate;
        this.objectMapper = objectMapper;
        this.resultProcessingService = resultProcessingService;
    }

    @Scheduled(fixedRate = 30000) // Every 30 seconds
    public void pollLiveEvents() {
        try {
            long now = System.currentTimeMillis();

            // --- Circuit breaker check ---
            if (circuitBreakerOpenUntilMs > 0 && now < circuitBreakerOpenUntilMs) {
                return; // silently skip while circuit breaker is open
            }
            if (circuitBreakerOpenUntilMs > 0 && now >= circuitBreakerOpenUntilMs) {
                log.info("ESPN circuit breaker CLOSED — attempting recovery poll");
                circuitBreakerOpenUntilMs = 0;
                // allow one attempt; if it fails, the circuit breaker re-opens below
            }

            // --- Backoff check ---
            if (now < nextAllowedPollEpochMs) {
                return; // still in backoff window
            }

            // --- Determine live event ---
            List<Event> upcomingEvents = eventRepository.findByStatus("UPCOMING");
            Event liveEvent = null;

            OffsetDateTime nowOdt = OffsetDateTime.now();
            for (Event event : upcomingEvents) {
                if (event.getEventDate() != null) {
                    OffsetDateTime eventTime = event.getEventDate();
                    // Event is "live" if started but within a reasonable window
                    if (nowOdt.isAfter(eventTime) && nowOdt.isBefore(eventTime.plusHours(LIVE_EVENT_WINDOW_HOURS))) {
                        // Check if all fights are already resolved
                        List<Fight> eventFights = fightRepository.findByEventIdOrderByFightOrderAsc(event.getId());
                        boolean allResolved = !eventFights.isEmpty() && eventFights.stream()
                                .allMatch(f -> "COMPLETED".equals(f.getStatus()) || "CANCELED".equals(f.getStatus()));
                        if (!allResolved) {
                            liveEvent = event;
                            break;
                        }
                    }
                }
            }

            if (liveEvent == null) {
                // No live event — throttle polling to once every 10 minutes
                if (now - lastNonLivePollEpochMs < NON_LIVE_POLL_INTERVAL_MS) {
                    return; // skip this tick
                }
                lastNonLivePollEpochMs = now;

                // Cleanup orphaned UPCOMING fights for events older than 24 hours
                List<Fight> orphanedFights = fightRepository.findByStatus("UPCOMING");
                for (Fight f : orphanedFights) {
                    eventRepository.findById(f.getEventId()).ifPresent(event -> {
                        if (event.getEventDate() != null && nowOdt.isAfter(event.getEventDate().plusHours(24))) {
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
            String response = espnRestTemplate.getForObject(ESPN_SCOREBOARD_URL, String.class);

            // --- Response validation ---
            if (response == null || response.isBlank()) {
                handleFailure("ESPN returned null/empty response");
                return;
            }
            String trimmed = response.trim();
            if (!trimmed.startsWith("{")) {
                // Likely an HTML error page (Akamai block, etc.)
                handleFailure("ESPN returned non-JSON response (likely blocked): " +
                        trimmed.substring(0, Math.min(200, trimmed.length())));
                return;
            }

            JsonNode root = objectMapper.readTree(response);
            if (!root.has("events")) {
                handleFailure("ESPN JSON response missing 'events' key");
                return;
            }

            // --- Success: reset resilience counters ---
            if (consecutiveFailures > 0) {
                log.info("ESPN poll recovered after {} consecutive failures", consecutiveFailures);
            }
            consecutiveFailures = 0;
            nextAllowedPollEpochMs = 0;

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
            handleFailure("Error polling ESPN live events: " + e.getMessage());
        }
    }

    /**
     * Handles a polling failure: logs, increments the consecutive failure counter,
     * applies exponential backoff, and opens the circuit breaker if threshold is reached.
     */
    private void handleFailure(String message) {
        consecutiveFailures++;
        log.error("{} (failure #{} consecutive)", message, consecutiveFailures);

        // Exponential backoff: 2^failures * 30s, capped at 15 minutes
        long backoffSeconds = Math.min((long) Math.pow(2, consecutiveFailures) * 30, MAX_BACKOFF_SECONDS);
        nextAllowedPollEpochMs = System.currentTimeMillis() + (backoffSeconds * 1000);
        log.warn("ESPN backoff: next poll allowed in {} seconds", backoffSeconds);

        // Circuit breaker: after N consecutive failures, pause for a long cooldown
        if (consecutiveFailures >= CIRCUIT_BREAKER_THRESHOLD) {
            circuitBreakerOpenUntilMs = System.currentTimeMillis() + CIRCUIT_BREAKER_COOLDOWN_MS;
            log.warn("ESPN circuit breaker OPEN — pausing polling for 30 minutes after {} consecutive failures",
                    consecutiveFailures);
        }
    }
}
