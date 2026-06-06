package com.valihameed.ufcfightpredictor.repository;

import com.valihameed.ufcfightpredictor.models.Event;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
public class EventRepositoryTest {

    @Autowired
    private EventRepository eventRepository;

    private Event event1;
    private Event event2;
    private Event event3;

    @BeforeEach
    void setUp() {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);

        event1 = Event.builder()
                .name("UFC 300")
                .eventDate(now.plusDays(10))
                .location("Las Vegas")
                .status("UPCOMING")
                .build();

        event2 = Event.builder()
                .name("UFC Fight Night: Holloway")
                .eventDate(now.minusDays(5))
                .location("New York")
                .status("COMPLETED")
                .build();

        event3 = Event.builder()
                .name("UFC 301")
                .eventDate(now.plusDays(30))
                .location("Rio de Janeiro")
                .status("UPCOMING")
                .build();

        eventRepository.saveAll(List.of(event1, event2, event3));
    }

    @Test
    void itShouldFindByName() {
        Optional<Event> foundEvent = eventRepository.findByName("UFC 300");
        assertThat(foundEvent).isPresent();
        assertThat(foundEvent.get().getLocation()).isEqualTo("Las Vegas");
    }

    @Test
    void itShouldFindByStatus() {
        List<Event> upcomingEvents = eventRepository.findByStatus("UPCOMING");
        assertThat(upcomingEvents).hasSize(2);
        assertThat(upcomingEvents).extracting(Event::getName).containsExactlyInAnyOrder("UFC 300", "UFC 301");
    }

    @Test
    void itShouldFindByEventDateBetweenAndStatus() {
        OffsetDateTime start = OffsetDateTime.now(ZoneOffset.UTC).plusDays(5);
        OffsetDateTime end = OffsetDateTime.now(ZoneOffset.UTC).plusDays(15);

        List<Event> events = eventRepository.findByEventDateBetweenAndStatus(start, end, "UPCOMING");

        assertThat(events).hasSize(1);
        assertThat(events.get(0).getName()).isEqualTo("UFC 300");
    }

    @Test
    void itShouldFindByStatusOrderByEventDateAsc() {
        List<Event> events = eventRepository.findByStatusOrderByEventDateAsc("UPCOMING", PageRequest.of(0, 10));
        
        assertThat(events).hasSize(2);
        assertThat(events.get(0).getName()).isEqualTo("UFC 300"); // Closer date
        assertThat(events.get(1).getName()).isEqualTo("UFC 301"); // Further date
    }

    @Test
    void itShouldFindByStatusOrderByEventDateDesc() {
        List<Event> events = eventRepository.findByStatusOrderByEventDateDesc("UPCOMING", PageRequest.of(0, 10));
        
        assertThat(events).hasSize(2);
        assertThat(events.get(0).getName()).isEqualTo("UFC 301"); // Further date first
        assertThat(events.get(1).getName()).isEqualTo("UFC 300"); // Closer date second
    }
}
