package com.valihameed.ufcfightpredictor.leaderboard;

import com.valihameed.ufcfightpredictor.models.Leaderboard;
import com.valihameed.ufcfightpredictor.repository.LeaderboardRepository;
import com.valihameed.ufcfightpredictor.repository.userRepository;
import com.valihameed.ufcfightpredictor.users.user;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/leaderboard")
@AllArgsConstructor
public class LeaderboardController {
    private final LeaderboardRepository leaderboardRepository;
    private final userRepository userRepository;
    private final com.valihameed.ufcfightpredictor.repository.PredictionResultRepository predictionResultRepository;

    @GetMapping
    public ResponseEntity<List<LeaderboardResponseDto>> global(@RequestParam(defaultValue = "0") int page) {
        var pageReq = PageRequest.of(page, 25);
        Page<Leaderboard> p = leaderboardRepository.findPublicLeaderboard(pageReq);
        
        List<LeaderboardResponseDto> dtos = p.getContent().stream().map(lb -> {
            LeaderboardResponseDto dto = new LeaderboardResponseDto();
            dto.setId(lb.getId());
            dto.setUserId(lb.getUserId());
            dto.setTotalPoints(lb.getTotalPoints());
            dto.setCorrectPredictions(lb.getCorrectPredictions());
            dto.setTotalPredictions(lb.getTotalPredictions());
            dto.setCurrentStreak(lb.getCurrentStreak());
            dto.setBestStreak(lb.getBestStreak());
            dto.setLastUpdated(lb.getLastUpdated());
            
            userRepository.findById(lb.getUserId()).ifPresent(u -> {
                dto.setUsername(u.getUsername());
            });
            return dto;
        }).collect(Collectors.toList());
        
        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<Leaderboard> byUser(@PathVariable Long userId) {
        return ResponseEntity.of(leaderboardRepository.findByUserId(userId));
    }
    
    @GetMapping("/event/{eventId}")
    public ResponseEntity<List<LeaderboardResponseDto>> byEvent(
            @PathVariable Long eventId) {
        
        List<Object[]> data = predictionResultRepository.getEventLeaderboardData(eventId);
        List<LeaderboardResponseDto> dtos = data.stream().map(row -> {
            LeaderboardResponseDto dto = new LeaderboardResponseDto();
            dto.setUserId(row[0] != null ? ((Number) row[0]).longValue() : null);
            dto.setUsername((String) row[1]);
            dto.setTotalPoints(row[2] != null ? ((Number) row[2]).intValue() : 0);
            dto.setCorrectPredictions(row[3] != null ? ((Number) row[3]).intValue() : 0);
            dto.setTotalPredictions(row[4] != null ? ((Number) row[4]).intValue() : 0);
            dto.setCurrentStreak(0);
            dto.setBestStreak(0);
            return dto;
        }).collect(Collectors.toList());
        
        return ResponseEntity.ok(dtos);
    }

    @Data
    public static class LeaderboardResponseDto {
        private Long id;
        private Long userId;
        private String username;
        private Integer totalPoints;
        private Integer correctPredictions;
        private Integer totalPredictions;
        private Integer currentStreak;
        private Integer bestStreak;
        private OffsetDateTime lastUpdated;
    }
}
