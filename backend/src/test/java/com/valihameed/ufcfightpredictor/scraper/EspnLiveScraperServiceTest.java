package com.valihameed.ufcfightpredictor.scraper;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.valihameed.ufcfightpredictor.models.Event;
import com.valihameed.ufcfightpredictor.models.Fight;
import com.valihameed.ufcfightpredictor.repository.EventRepository;
import com.valihameed.ufcfightpredictor.repository.FightRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestTemplate;

import java.time.OffsetDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EspnLiveScraperServiceTest {

    @Mock
    private EventRepository eventRepository;

    @Mock
    private FightRepository fightRepository;

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private ObjectMapper objectMapper;

    private EspnLiveScraperService espnLiveScraperService;

    @BeforeEach
    void setUp() {
        // Use a real ObjectMapper for parsing the mocked JSON
        espnLiveScraperService = new EspnLiveScraperService(eventRepository, fightRepository, restTemplate, new ObjectMapper());
    }

    @Test
    void pollLiveEvents_NoUpcomingEvents_DoesNothing() {
        when(eventRepository.findByStatus("UPCOMING")).thenReturn(List.of());

        espnLiveScraperService.pollLiveEvents();

        verify(restTemplate, never()).getForObject(anyString(), eq(String.class));
    }

    @Test
    void pollLiveEvents_EventNotStarted_DoesNothing() {
        Event futureEvent = new Event();
        futureEvent.setName("UFC 300");
        futureEvent.setEventDate(OffsetDateTime.now().plusDays(1)); // Future

        when(eventRepository.findByStatus("UPCOMING")).thenReturn(List.of(futureEvent));

        espnLiveScraperService.pollLiveEvents();

        verify(restTemplate, never()).getForObject(anyString(), eq(String.class));
    }

    @Test
    void pollLiveEvents_EventStarted_UpdatesFights() {
        Event liveEvent = new Event();
        liveEvent.setId(1L);
        liveEvent.setName("UFC 300");
        liveEvent.setEventDate(OffsetDateTime.now().minusHours(1)); // Past

        Fight fight = new Fight();
        fight.setId(10L);
        fight.setFighter1Name("Steve Garcia");
        fight.setFighter2Name("Diego Lopes");
        fight.setEventId(1L);

        when(eventRepository.findByStatus("UPCOMING")).thenReturn(List.of(liveEvent));
        when(fightRepository.findByEventIdOrderByFightOrderAsc(1L)).thenReturn(List.of(fight));

        String mockEspnJson = """
        {
          "events": [
            {
              "competitions": [
                {
                  "competitors": [
                    { "athlete": { "fullName": "Steven Garcia" }, "winner": false },
                    { "athlete": { "fullName": "Diego Lopes" }, "winner": false }
                  ],
                  "status": {
                    "clock": 135.0,
                    "displayClock": "2:15",
                    "period": 2,
                    "type": {
                      "completed": false,
                      "state": "in",
                      "name": "STATUS_IN_PROGRESS"
                    }
                  }
                }
              ]
            }
          ]
        }
        """;

        when(restTemplate.getForObject(anyString(), eq(String.class))).thenReturn(mockEspnJson);

        espnLiveScraperService.pollLiveEvents();

        ArgumentCaptor<List<Fight>> fightCaptor = ArgumentCaptor.forClass(List.class);
        verify(fightRepository).saveAll(fightCaptor.capture());

        List<Fight> savedFights = fightCaptor.getValue();
        assertEquals(1, savedFights.size());
        Fight savedFight = savedFights.get(0);
        assertEquals(2, savedFight.getCurrentRound());
        assertEquals("2:15", savedFight.getCurrentClock());
        assertEquals("STATUS_IN_PROGRESS", savedFight.getLiveStatus());
        assertNull(savedFight.getResultWinner()); // Not completed
    }

    @Test
    void pollLiveEvents_FightCompleted_UpdatesOfficialResults() {
        Event liveEvent = new Event();
        liveEvent.setId(1L);
        liveEvent.setName("UFC 300");
        liveEvent.setEventDate(OffsetDateTime.now().minusHours(2));

        Fight fight = new Fight();
        fight.setId(10L);
        fight.setFighter1Name("Alex Pereira");
        fight.setFighter2Name("Jamahal Hill");
        fight.setEventId(1L);

        when(eventRepository.findByStatus("UPCOMING")).thenReturn(List.of(liveEvent));
        when(fightRepository.findByEventIdOrderByFightOrderAsc(1L)).thenReturn(List.of(fight));

        String mockEspnJson = """
        {
          "events": [
            {
              "competitions": [
                {
                  "competitors": [
                    { "athlete": { "fullName": "Alex Pereira" }, "winner": true },
                    { "athlete": { "fullName": "Jamahal Hill" }, "winner": false }
                  ],
                  "status": {
                    "clock": 0.0,
                    "displayClock": "0:00",
                    "period": 1,
                    "type": {
                      "completed": true,
                      "state": "post",
                      "name": "STATUS_FINAL",
                      "detail": "KO/TKO"
                    }
                  }
                }
              ]
            }
          ]
        }
        """;

        when(restTemplate.getForObject(anyString(), eq(String.class))).thenReturn(mockEspnJson);

        espnLiveScraperService.pollLiveEvents();

        ArgumentCaptor<List<Fight>> fightCaptor = ArgumentCaptor.forClass(List.class);
        verify(fightRepository).saveAll(fightCaptor.capture());

        List<Fight> savedFights = fightCaptor.getValue();
        assertEquals(1, savedFights.size());
        Fight savedFight = savedFights.get(0);
        
        assertEquals(1, savedFight.getCurrentRound());
        assertEquals("0:00", savedFight.getCurrentClock());
        assertEquals("STATUS_FINAL", savedFight.getLiveStatus());
        
        // Check official results
        assertEquals("Alex Pereira", savedFight.getResultWinner());
        assertEquals(1, savedFight.getResultRound());
        assertEquals("0:00", savedFight.getResultTime());
        assertEquals("KO/TKO", savedFight.getResultMethod());
    }
}
