package com.valihameed.ufcfightpredictor.admin;

import com.valihameed.ufcfightpredictor.rewards.RewardService;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin/rewards")
@AllArgsConstructor
public class AdminRewardController {

    private final RewardService rewardService;

    /**
     * Manually distribute rewards for a completed event.
     */
    @PostMapping("/event/{eventId}")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<Map<String, String>> distributeEventRewards(@PathVariable Long eventId) {
        rewardService.distributeEventRewards(eventId);
        return ResponseEntity.ok(Map.of("message", "Event rewards distributed for event " + eventId));
    }

    /**
     * Manually distribute rewards for a completed season.
     */
    @PostMapping("/season/{seasonId}")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<Map<String, String>> distributeSeasonRewards(@PathVariable Long seasonId) {
        rewardService.distributeSeasonRewards(seasonId);
        return ResponseEntity.ok(Map.of("message", "Season rewards distributed for season " + seasonId));
    }
}
