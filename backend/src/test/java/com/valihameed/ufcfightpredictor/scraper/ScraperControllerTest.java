package com.valihameed.ufcfightpredictor.scraper;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.valihameed.ufcfightpredictor.models.Event;
import com.valihameed.ufcfightpredictor.models.Fight;
import com.valihameed.ufcfightpredictor.repository.*;
import com.valihameed.ufcfightpredictor.results.ResultProcessingService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ScraperController.class)
@AutoConfigureMockMvc(addFilters = false)
@org.springframework.test.context.ActiveProfiles("test")
public class ScraperControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean private EventRepository eventRepository;
    @MockBean private FightRepository fightRepository;
    @MockBean private ForumThreadRepository forumThreadRepository;
    @MockBean private UserPredictionRepository userPredictionRepository;
    @MockBean private NotificationRepository notificationRepository;
    @MockBean private com.valihameed.ufcfightpredictor.notifications.NotificationService notificationService;
    @MockBean private userRepository userRepository;
    @MockBean private ResultProcessingService resultProcessingService;
    @MockBean private com.valihameed.ufcfightpredictor.security.JwtService jwtService;
    @MockBean private org.springframework.security.core.userdetails.UserDetailsService userDetailsService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void canUpsertEvents() throws Exception {
        // Given
        Event event = new Event();
        event.setName("UFC 300");

        Event savedEvent = new Event();
        savedEvent.setId(100L);
        savedEvent.setName("UFC 300");

        given(eventRepository.findByName("UFC 300")).willReturn(Optional.empty());
        given(eventRepository.save(any(Event.class))).willReturn(savedEvent);

        // When / Then
        mockMvc.perform(post("/api/v1/internal/scraper/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(List.of(event))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("UFC 300"));
    }

    @Test
    void canUpsertFightsAndProcessResults() throws Exception {
        // Given
        Fight fight = new Fight();
        fight.setEventId(100L);
        fight.setFighter1Name("Conor");
        fight.setFighter2Name("Dustin");
        fight.setStatus("COMPLETED");
        fight.setResultWinner("Conor");

        Fight savedFight = new Fight();
        savedFight.setId(10L);
        savedFight.setEventId(100L);
        savedFight.setFighter1Name("Conor");
        savedFight.setFighter2Name("Dustin");
        savedFight.setStatus("COMPLETED");
        savedFight.setResultWinner("Conor");

        given(fightRepository.findByEventIdAndFighter1NameAndFighter2Name(100L, "Conor", "Dustin")).willReturn(Optional.empty());
        given(fightRepository.save(any(Fight.class))).willReturn(savedFight);

        // When / Then
        mockMvc.perform(post("/api/v1/internal/scraper/fights")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(List.of(fight))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].resultWinner").value("Conor"));
    }
}
