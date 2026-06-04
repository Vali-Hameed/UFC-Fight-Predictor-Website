package com.valihameed.ufcfightpredictor.events;

import com.valihameed.ufcfightpredictor.models.Event;
import com.valihameed.ufcfightpredictor.repository.EventRepository;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@AllArgsConstructor
public class EventService {
    private final EventRepository eventRepository;

    public List<Event> listAll() {
        List<Event> completedEvents = eventRepository.findByStatusOrderByEventDateDesc("COMPLETED", PageRequest.of(0, 1));
        List<Event> upcomingEvents = eventRepository.findByStatusOrderByEventDateAsc("UPCOMING", PageRequest.of(0, 5));
        
        List<Event> combined = new ArrayList<>();
        combined.addAll(completedEvents);
        combined.addAll(upcomingEvents);
        
        return combined;
    }

    public Optional<Event> findById(Long id) { return eventRepository.findById(id); }

    public Event create(Event event) { return eventRepository.save(event); }

    public Event update(Long id, Event updated) {
        return eventRepository.findById(id).map(e -> {
            e.setName(updated.getName());
            e.setEventDate(updated.getEventDate());
            e.setLocation(updated.getLocation());
            e.setStatus(updated.getStatus());
            return eventRepository.save(e);
        }).orElseThrow(() -> new RuntimeException("Event not found"));
    }

    public void delete(Long id) { eventRepository.deleteById(id); }
}
