package com.valihameed.ufcfightpredictor.fights;

import com.valihameed.ufcfightpredictor.models.Fight;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/v1")
@AllArgsConstructor
public class FightController {
    private final FightService fightService;

    @GetMapping("/events/{eventId}/fights")
    public List<Fight> listByEvent(@PathVariable Long eventId) { return fightService.findByEventId(eventId); }

    @PostMapping("/events/{eventId}/fights")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<Fight> create(@PathVariable Long eventId, @RequestBody Fight fight) {
        fight.setEventId(eventId);
        Fight created = fightService.create(fight);
        return ResponseEntity.created(URI.create("/api/v1/fights/" + created.getId())).body(created);
    }

    @PutMapping("/fights/{id}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public Fight update(@PathVariable Long id, @RequestBody Fight fight) {
        return fightService.update(id,fight);
    }
}
