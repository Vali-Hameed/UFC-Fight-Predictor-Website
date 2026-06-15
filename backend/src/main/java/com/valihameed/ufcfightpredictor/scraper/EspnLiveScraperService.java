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
                    Fight matchedFight = fuzzyMatchFight(dbFights, f1Name, f2Name);
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

                        if (completed && matchedFight.getResultWinner() == null) {
                            // Fight just finished, update official results!
                            log.info("Fight completed on ESPN: {} vs {}", f1Name, f2Name);
                            matchedFight.setResultRound(period);
                            matchedFight.setResultTime(displayClock);
                            
                            // Determine winner
                            boolean f1Winner = competitors.get(0).path("winner").asBoolean(false);
                            boolean f2Winner = competitors.get(1).path("winner").asBoolean(false);

                            if (f1Winner) {
                                matchedFight.setResultWinner(matchedFight.getFighter1Name()); // use DB name for consistency
                            } else if (f2Winner) {
                                matchedFight.setResultWinner(matchedFight.getFighter2Name());
                            } else {
                                matchedFight.setResultWinner("Draw/NC");
                            }
                            
                            // Try to get method (often in 'competitions[0].status.type.detail' or 'notes')
                            String detail = typeNode.path("detail").asText("");
                            matchedFight.setResultMethod(detail);
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

    private Fight fuzzyMatchFight(List<Fight> dbFights, String espnF1, String espnF2) {
        for (Fight fight : dbFights) {
            String dbF1 = fight.getFighter1Name();
            String dbF2 = fight.getFighter2Name();
            
            if (isMatch(dbF1, espnF1) || isMatch(dbF1, espnF2) || isMatch(dbF2, espnF1) || isMatch(dbF2, espnF2)) {
                return fight;
            }
        }
        return null;
    }

    private boolean isMatch(String dbName, String espnName) {
        if (dbName == null || espnName == null) return false;
        if (dbName.equalsIgnoreCase(espnName)) return true;
        
        // Match by last name
        String[] dbParts = dbName.split(" ");
        String[] espnParts = espnName.split(" ");
        
        if (dbParts.length > 0 && espnParts.length > 0) {
            String dbLast = dbParts[dbParts.length - 1];
            String espnLast = espnParts[espnParts.length - 1];
            if (dbLast.equalsIgnoreCase(espnLast)) {
                return true;
            }
        }
        return false;
    }
}
