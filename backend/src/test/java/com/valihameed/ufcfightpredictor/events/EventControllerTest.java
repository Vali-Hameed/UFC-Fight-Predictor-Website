package com.valihameed.ufcfightpredictor.events;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.valihameed.ufcfightpredictor.models.Event;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(EventController.class)
@AutoConfigureMockMvc(addFilters = false)
@org.springframework.test.context.ActiveProfiles("test")
public class EventControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private EventService eventService;

    @MockBean
    private com.valihameed.ufcfightpredictor.security.JwtService jwtService;

    @MockBean
    private org.springframework.security.core.userdetails.UserDetailsService userDetailsService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void canListEvents() throws Exception {
        // Given
        Event event1 = new Event();
        event1.setId(1L);
        event1.setName("UFC 300");

        Event event2 = new Event();
        event2.setId(2L);
        event2.setName("UFC 301");

        given(eventService.listAll()).willReturn(List.of(event1, event2));

        // When / Then
        mockMvc.perform(get("/api/v1/events"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size()").value(2))
                .andExpect(jsonPath("$[0].name").value("UFC 300"))
                .andExpect(jsonPath("$[1].name").value("UFC 301"));
    }

    @Test
    void canGetEventById() throws Exception {
        // Given
        Event event = new Event();
        event.setId(1L);
        event.setName("UFC 300");

        given(eventService.findById(1L)).willReturn(Optional.of(event));

        // When / Then
        mockMvc.perform(get("/api/v1/events/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("UFC 300"));
    }

    @Test
    void getEventByIdReturnsNotFoundWhenNotExists() throws Exception {
        // Given
        given(eventService.findById(1L)).willReturn(Optional.empty());

        // When / Then
        mockMvc.perform(get("/api/v1/events/1"))
                .andExpect(status().isNotFound());
    }

    @Test
    void canCreateEvent() throws Exception {
        // Given
        Event eventToCreate = new Event();
        eventToCreate.setName("UFC 300");

        Event createdEvent = new Event();
        createdEvent.setId(1L);
        createdEvent.setName("UFC 300");

        given(eventService.create(any(Event.class))).willReturn(createdEvent);

        // When / Then
        mockMvc.perform(post("/api/v1/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(eventToCreate)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/v1/events/1"))
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("UFC 300"));
    }

    @Test
    void canUpdateEvent() throws Exception {
        // Given
        Event updatedEvent = new Event();
        updatedEvent.setName("UFC 300 - Updated");

        given(eventService.update(eq(1L), any(Event.class))).willReturn(updatedEvent);

        // When / Then
        mockMvc.perform(put("/api/v1/events/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updatedEvent)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("UFC 300 - Updated"));
    }

    @Test
    void canDeleteEvent() throws Exception {
        // When / Then
        mockMvc.perform(delete("/api/v1/events/1"))
                .andExpect(status().isNoContent());
    }
}
