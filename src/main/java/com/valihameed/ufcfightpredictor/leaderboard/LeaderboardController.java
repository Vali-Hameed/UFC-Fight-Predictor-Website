package com.valihameed.ufcfightpredictor.leaderboard;

import com.valihameed.ufcfightpredictor.models.Leaderboard;
import com.valihameed.ufcfightpredictor.repository.LeaderboardRepository;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/leaderboard")
@AllArgsConstructor
public class LeaderboardController {
    private final LeaderboardRepository leaderboardRepository;

    @GetMapping
    public ResponseEntity<List<Leaderboard>> global(@RequestParam(defaultValue = "0") int page) {
        var pageReq = PageRequest.of(page, 25);
        var p = leaderboardRepository.findAll(pageReq);
        return ResponseEntity.ok(p.getContent());
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<Leaderboard> byUser(@PathVariable Long userId) {
        return ResponseEntity.of(leaderboardRepository.findByUserId(userId));
    }
}
