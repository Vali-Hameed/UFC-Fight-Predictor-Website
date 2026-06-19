package com.valihameed.ufcfightpredictor.leaderboard;

import com.valihameed.ufcfightpredictor.models.*;
import com.valihameed.ufcfightpredictor.repository.*;
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
    private final PredictionResultRepository predictionResultRepository;
    private final SeasonLeaderboardRepository seasonLeaderboardRepository;
    private final EventLeaderboardRepository eventLeaderboardRepository;
    private final SeasonRepository seasonRepository;
    private final EventRepository eventRepository;
    private final UserBadgeRepository userBadgeRepository;

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
            
            enrichWithUserCosmetics(dto, lb.getUserId());
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
        
        var topEntries = eventLeaderboardRepository.findPublicByEventId(eventId, PageRequest.of(0, 50));
        List<LeaderboardResponseDto> dtos = topEntries.getContent().stream().map(elb -> {
            LeaderboardResponseDto dto = new LeaderboardResponseDto();
            dto.setUserId(elb.getUserId());
            dto.setTotalPoints(elb.getTotalPoints());
            dto.setCorrectPredictions(elb.getCorrectPredictions());
            dto.setTotalPredictions(elb.getTotalPredictions());
            dto.setCurrentStreak(0);
            dto.setBestStreak(0);
            
            enrichWithUserCosmetics(dto, elb.getUserId());
            return dto;
        }).collect(Collectors.toList());
        
        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/season/{seasonId}")
    public ResponseEntity<List<LeaderboardResponseDto>> bySeason(
            @PathVariable Long seasonId,
            @RequestParam(defaultValue = "0") int page) {
        var pageReq = PageRequest.of(page, 25);
        Page<SeasonLeaderboard> p = seasonLeaderboardRepository.findPublicBySeasonId(seasonId, pageReq);

        List<LeaderboardResponseDto> dtos = p.getContent().stream().map(slb -> {
            LeaderboardResponseDto dto = new LeaderboardResponseDto();
            dto.setUserId(slb.getUserId());
            dto.setTotalPoints(slb.getTotalPoints());
            dto.setCorrectPredictions(slb.getCorrectPredictions());
            dto.setTotalPredictions(slb.getTotalPredictions());
            dto.setCurrentStreak(slb.getCurrentStreak());
            dto.setBestStreak(slb.getBestStreak());
            dto.setLastUpdated(slb.getLastUpdated());

            enrichWithUserCosmetics(dto, slb.getUserId());
            return dto;
        }).collect(Collectors.toList());

        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/season/name/{seasonName}")
    public ResponseEntity<List<LeaderboardResponseDto>> bySeasonName(
            @PathVariable String seasonName,
            @RequestParam(defaultValue = "0") int page) {
        return seasonRepository.findByName(seasonName)
                .map(season -> bySeason(season.getId(), page))
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Returns available filters for the leaderboard UI: seasons and recent completed events.
     */
    @GetMapping("/filters")
    public ResponseEntity<LeaderboardFiltersDto> filters() {
        LeaderboardFiltersDto dto = new LeaderboardFiltersDto();

        // All seasons
        List<SeasonFilterDto> seasons = seasonRepository.findAllByOrderByStartDateDesc()
                .stream().map(s -> {
                    SeasonFilterDto sf = new SeasonFilterDto();
                    sf.setId(s.getId());
                    sf.setName(s.getName());
                    sf.setActive(s.isActive());
                    sf.setChampionUserId(s.getChampionUserId());
                    if (s.getChampionUserId() != null) {
                        userRepository.findById(s.getChampionUserId())
                                .ifPresent(u -> sf.setChampionUsername(u.getUsername()));
                    }
                    return sf;
                }).collect(Collectors.toList());
        dto.setSeasons(seasons);

        // Recent completed events (last 10)
        var recentEvents = eventRepository.findAllByEventDateBeforeOrderByEventDateDesc(
                OffsetDateTime.now(), PageRequest.of(0, 10));
        List<EventFilterDto> events = recentEvents.getContent().stream().map(e -> {
            EventFilterDto ef = new EventFilterDto();
            ef.setId(e.getId());
            ef.setName(e.getName());
            ef.setEventDate(e.getEventDate());
            ef.setStatus(e.getStatus());
            return ef;
        }).collect(Collectors.toList());
        dto.setRecentEvents(events);

        return ResponseEntity.ok(dto);
    }

    private void enrichWithUserCosmetics(LeaderboardResponseDto dto, Long userId) {
        userRepository.findById(userId).ifPresent(u -> {
            dto.setUsername(u.getUsername());
            dto.setCosmeticGlowColor(u.getCosmeticGlowColor());
            dto.setCosmeticTitle(u.getCosmeticTitle());
        });
        List<UserBadge> badges = userBadgeRepository.findByUserId(userId);
        if (!badges.isEmpty()) {
            dto.setBadges(badges.stream().map(b -> {
                BadgeDto bd = new BadgeDto();
                bd.setId(b.getId());
                bd.setBadgeType(b.getBadgeType());
                bd.setBadgeLabel(b.getBadgeLabel());
                bd.setAwardedAt(b.getAwardedAt());
                return bd;
            }).collect(Collectors.toList()));
        }
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
        // Cosmetics
        private String cosmeticGlowColor;
        private String cosmeticTitle;
        private List<BadgeDto> badges;
    }

    @Data
    public static class BadgeDto {
        private Long id;
        private String badgeType;
        private String badgeLabel;
        private OffsetDateTime awardedAt;
    }

    @Data
    public static class LeaderboardFiltersDto {
        private List<SeasonFilterDto> seasons;
        private List<EventFilterDto> recentEvents;
    }

    @Data
    public static class SeasonFilterDto {
        private Long id;
        private String name;
        private boolean active;
        private Long championUserId;
        private String championUsername;
    }

    @Data
    public static class EventFilterDto {
        private Long id;
        private String name;
        private OffsetDateTime eventDate;
        private String status;
    }
}
