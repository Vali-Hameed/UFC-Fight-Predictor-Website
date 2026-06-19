package com.valihameed.ufcfightpredictor.rewards;

import com.valihameed.ufcfightpredictor.models.*;
import com.valihameed.ufcfightpredictor.repository.*;
import com.valihameed.ufcfightpredictor.users.user;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

@Service
@AllArgsConstructor
public class RewardService {
    private static final Logger log = LoggerFactory.getLogger(RewardService.class);

    private final EventLeaderboardRepository eventLeaderboardRepository;
    private final SeasonLeaderboardRepository seasonLeaderboardRepository;
    private final UserBadgeRepository userBadgeRepository;
    private final SeasonRepository seasonRepository;
    private final EventRepository eventRepository;
    private final userRepository userRepository;
    private final com.valihameed.ufcfightpredictor.notifications.NotificationService notificationService;
    private final NotificationRepository notificationRepository;
    private final jakarta.persistence.EntityManager entityManager;

    /**
     * Award badges for the top predictor of a completed event.
     * Called automatically when an event is marked COMPLETED, or manually by admin.
     * Event winner glow is TEMPORARY — it gets cleared when the next event's rewards are distributed.
     */
    @Transactional
    public void distributeEventRewards(Long eventId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new IllegalArgumentException("Event not found: " + eventId));

        // --- Clear previous event winner glows (temporary cosmetics expire here) ---
        List<user> previousEventGlowUsers = userRepository.findByCosmeticGlowSourceEventIdNotNull();
        for (user u : previousEventGlowUsers) {
            if (!u.getCosmeticGlowSourceEventId().equals(eventId)) {
                u.setCosmeticGlowColor(null);
                u.setCosmeticGlowSourceEventId(null);
                userRepository.save(u);
                log.info("Cleared temporary event glow from user {} (was from event {})", u.getId(), eventId);
            }
        }

        var topEntries = eventLeaderboardRepository.findPublicByEventId(eventId, PageRequest.of(0, 3));
        if (topEntries.isEmpty()) {
            log.info("No event leaderboard data for event {}, skipping reward distribution.", eventId);
            return;
        }

        EventLeaderboard winner = topEntries.getContent().get(0);
        if (winner.getTotalPoints() <= 0) {
            log.info("Event {} winner has 0 points, skipping reward.", eventId);
            return;
        }

        // Award EVENT_WINNER badge if not already awarded
        if (!userBadgeRepository.existsByUserIdAndBadgeTypeAndEventId(winner.getUserId(), "EVENT_WINNER", eventId)) {
            UserBadge badge = UserBadge.builder()
                    .userId(winner.getUserId())
                    .badgeType("EVENT_WINNER")
                    .badgeLabel(event.getName() + " Winner")
                    .eventId(eventId)
                    .awardedAt(OffsetDateTime.now())
                    .build();
            userBadgeRepository.save(badge);

            // Update cosmetic title and temporary glow
            long eventWins = userBadgeRepository.countByUserIdAndBadgeType(winner.getUserId(), "EVENT_WINNER");
            userRepository.findById(winner.getUserId()).ifPresent(u -> {
                // If user doesn't have an active season glow (which is higher priority), apply event glow
                if (u.getCosmeticGlowSourceSeasonId() == null) {
                    u.setCosmeticTitle(eventWins + "x Event Winner");
                    u.setCosmeticGlowColor("linear-gradient(90deg, #E53E3E, #B91C1C)");
                    u.setCosmeticGlowSourceEventId(eventId);
                    userRepository.save(u);
                }
            });

            // Notify the winner
            Notification note = Notification.builder()
                    .userId(winner.getUserId())
                    .type("REWARD")
                    .message("🏆 Congratulations! You won the " + event.getName() + " prediction event! Your name glows red until the next event!")
                    .build();
            notificationService.createNotification(note);

            log.info("Awarded EVENT_WINNER badge to user {} for event {}", winner.getUserId(), event.getName());
        }

        // Check for PERFECT_EVENT (100% accuracy with at least 3 predictions)
        if (winner.getCorrectPredictions() != null && winner.getTotalPredictions() != null
                && winner.getCorrectPredictions().equals(winner.getTotalPredictions())
                && winner.getTotalPredictions() >= 3) {
            if (!userBadgeRepository.existsByUserIdAndBadgeTypeAndEventId(winner.getUserId(), "PERFECT_EVENT", eventId)) {
                UserBadge perfectBadge = UserBadge.builder()
                        .userId(winner.getUserId())
                        .badgeType("PERFECT_EVENT")
                        .badgeLabel(event.getName() + " Perfect Pick")
                        .eventId(eventId)
                        .awardedAt(OffsetDateTime.now())
                        .build();
                userBadgeRepository.save(perfectBadge);
                log.info("Awarded PERFECT_EVENT badge to user {} for event {}", winner.getUserId(), event.getName());
            }
        }
    }

    /**
     * Award badges for the season champion and top-3 finishers.
     * Called when a season ends or manually by admin.
     * Season cosmetics are TEMPORARY — they get cleared when the next season's rewards are distributed.
     */
    @Transactional
    public void distributeSeasonRewards(Long seasonId) {
        Season season = seasonRepository.findById(seasonId)
                .orElseThrow(() -> new IllegalArgumentException("Season not found: " + seasonId));

        // --- Clear previous season glows (temporary cosmetics expire here) ---
        List<user> previousSeasonGlowUsers = userRepository.findByCosmeticGlowSourceSeasonIdNotNull();
        for (user u : previousSeasonGlowUsers) {
            if (!u.getCosmeticGlowSourceSeasonId().equals(seasonId)) {
                u.setCosmeticGlowColor(null);
                u.setCosmeticGlowSourceSeasonId(null);
                userRepository.save(u);
                log.info("Cleared temporary season glow from user {} (was from season {})", u.getId(), seasonId);
            }
        }

        var topEntries = seasonLeaderboardRepository.findPublicBySeasonId(seasonId, PageRequest.of(0, 3));
        if (topEntries.isEmpty()) {
            log.info("No season leaderboard data for season {}, skipping.", season.getName());
            return;
        }

        List<SeasonLeaderboard> topList = topEntries.getContent();

        // 1st Place (Gold)
        if (topList.size() > 0) {
            SeasonLeaderboard champion = topList.get(0);
            if (champion.getTotalPoints() > 0 && !userBadgeRepository.existsByUserIdAndBadgeTypeAndSeasonId(champion.getUserId(), "SEASON_CHAMPION", seasonId)) {
                UserBadge badge = UserBadge.builder()
                        .userId(champion.getUserId())
                        .badgeType("SEASON_CHAMPION")
                        .badgeLabel(season.getName() + " Champion")
                        .seasonId(seasonId)
                        .awardedAt(OffsetDateTime.now())
                        .build();
                userBadgeRepository.save(badge);

                season.setChampionUserId(champion.getUserId());
                seasonRepository.save(season);

                userRepository.findById(champion.getUserId()).ifPresent(u -> {
                    u.setCosmeticGlowColor("linear-gradient(90deg, #FFD700, #FFA500)");
                    u.setCosmeticTitle(season.getName() + " Champion");
                    u.setCosmeticGlowSourceEventId(null); // Overwrite event glow if any
                    u.setCosmeticGlowSourceSeasonId(seasonId); // Temporary season reward
                    userRepository.save(u);
                });

                Notification note = Notification.builder()
                        .userId(champion.getUserId())
                        .type("REWARD")
                        .message("👑 You are the " + season.getName() + " Champion! Your name now glows gold across the platform.")
                        .build();
                notificationService.createNotification(note);
            }
        }

        // 2nd Place (Silver)
        if (topList.size() > 1) {
            SeasonLeaderboard second = topList.get(1);
            if (second.getTotalPoints() > 0 && !userBadgeRepository.existsByUserIdAndBadgeTypeAndSeasonId(second.getUserId(), "SEASON_SILVER", seasonId)) {
                UserBadge badge = UserBadge.builder()
                        .userId(second.getUserId())
                        .badgeType("SEASON_SILVER")
                        .badgeLabel(season.getName() + " Silver")
                        .seasonId(seasonId)
                        .awardedAt(OffsetDateTime.now())
                        .build();
                userBadgeRepository.save(badge);

                userRepository.findById(second.getUserId()).ifPresent(u -> {
                    if (u.getCosmeticGlowSourceSeasonId() == null || !u.getCosmeticGlowColor().contains("#FFD700")) {
                        u.setCosmeticGlowColor("linear-gradient(90deg, #C0C0C0, #A9A9A9)");
                        u.setCosmeticTitle(season.getName() + " Silver");
                        u.setCosmeticGlowSourceEventId(null);
                        u.setCosmeticGlowSourceSeasonId(seasonId);
                        userRepository.save(u);
                    }
                });

                Notification note = Notification.builder()
                        .userId(second.getUserId())
                        .type("REWARD")
                        .message("🥈 You finished 2nd in " + season.getName() + "! Your name now glows silver.")
                        .build();
                notificationService.createNotification(note);
            }
        }

        // 3rd Place (Bronze)
        if (topList.size() > 2) {
            SeasonLeaderboard third = topList.get(2);
            if (third.getTotalPoints() > 0 && !userBadgeRepository.existsByUserIdAndBadgeTypeAndSeasonId(third.getUserId(), "SEASON_BRONZE", seasonId)) {
                UserBadge badge = UserBadge.builder()
                        .userId(third.getUserId())
                        .badgeType("SEASON_BRONZE")
                        .badgeLabel(season.getName() + " Bronze")
                        .seasonId(seasonId)
                        .awardedAt(OffsetDateTime.now())
                        .build();
                userBadgeRepository.save(badge);

                userRepository.findById(third.getUserId()).ifPresent(u -> {
                    if (u.getCosmeticGlowSourceSeasonId() == null || (!u.getCosmeticGlowColor().contains("#FFD700") && !u.getCosmeticGlowColor().contains("#C0C0C0"))) {
                        u.setCosmeticGlowColor("linear-gradient(90deg, #CD7F32, #8B4513)");
                        u.setCosmeticTitle(season.getName() + " Bronze");
                        u.setCosmeticGlowSourceEventId(null);
                        u.setCosmeticGlowSourceSeasonId(seasonId);
                        userRepository.save(u);
                    }
                });

                Notification note = Notification.builder()
                        .userId(third.getUserId())
                        .type("REWARD")
                        .message("🥉 You finished 3rd in " + season.getName() + "! Your name now glows bronze.")
                        .build();
                notificationService.createNotification(note);
            }
        }
    }

    /**
     * Check and award streak badges when a user's streak changes.
     */
    @Transactional
    public void checkStreakBadges(Long userId, int currentStreak) {
        if (currentStreak >= 25) {
            List<UserBadge> existing = userBadgeRepository.findByUserIdAndBadgeType(userId, "STREAK_25");
            if (existing.isEmpty()) {
                UserBadge badge = UserBadge.builder()
                        .userId(userId)
                        .badgeType("STREAK_25")
                        .badgeLabel("25+ Win Streak")
                        .awardedAt(OffsetDateTime.now())
                        .build();
                userBadgeRepository.save(badge);

                Notification note = Notification.builder()
                        .userId(userId)
                        .type("REWARD")
                        .message("🔥 Incredible! You hit a 25+ win streak! You've earned the legendary streak badge.")
                        .build();
                notificationService.createNotification(note);
            }
        } else if (currentStreak >= 10) {
            List<UserBadge> existing = userBadgeRepository.findByUserIdAndBadgeType(userId, "STREAK_10");
            if (existing.isEmpty()) {
                UserBadge badge = UserBadge.builder()
                        .userId(userId)
                        .badgeType("STREAK_10")
                        .badgeLabel("10+ Win Streak")
                        .awardedAt(OffsetDateTime.now())
                        .build();
                userBadgeRepository.save(badge);

                Notification note = Notification.builder()
                        .userId(userId)
                        .type("REWARD")
                        .message("🔥 You're on fire! 10+ win streak! You've earned the streak badge.")
                        .build();
                notificationService.createNotification(note);
            }
        }
    }

    /**
     * Find or create the current season based on current date.
     * Seasons run Jan 1 – Dec 31 of each year. Name format: SS{YY}
     */
    public Season getOrCreateCurrentSeason() {
        OffsetDateTime now = OffsetDateTime.now();
        int year = now.getYear();
        String seasonName = "SS" + (year % 100);

        return seasonRepository.findByName(seasonName).orElseGet(() -> {
            Season season = Season.builder()
                    .name(seasonName)
                    .startDate(OffsetDateTime.parse(year + "-01-01T00:00:00Z"))
                    .endDate(OffsetDateTime.parse(year + "-12-31T23:59:59Z"))
                    .active(true)
                    .build();
            return seasonRepository.save(season);
        });
    }

    /**
     * Backfill all event and season leaderboards based on existing prediction results,
     * and distribute rewards for all past events.
     */
    @Transactional
    public void backfillAllRewards() {
        log.info("Starting backfill of leaderboards and rewards...");

        // 1. Clear existing
        entityManager.createNativeQuery("TRUNCATE TABLE event_leaderboards, season_leaderboards RESTART IDENTITY").executeUpdate();
        log.info("Cleared existing event and season leaderboards.");

        // 2. Populate Event Leaderboards
        String eventSql = "INSERT INTO event_leaderboards (event_id, user_id, total_points, correct_predictions, total_predictions, last_updated) " +
            "SELECT f.event_id, up.user_id, SUM(pr.points_awarded), SUM(CASE WHEN pr.points_awarded > 0 THEN 1 ELSE 0 END), COUNT(up.id), NOW() " +
            "FROM prediction_results pr " +
            "JOIN user_predictions up ON pr.user_prediction_id = up.id " +
            "JOIN fights f ON up.fight_id = f.id " +
            "WHERE f.event_id IS NOT NULL " +
            "GROUP BY f.event_id, up.user_id";
        int eventRows = entityManager.createNativeQuery(eventSql).executeUpdate();
        log.info("Inserted {} rows into event_leaderboards.", eventRows);

        // 3. Populate Season Leaderboards
        Season season = getOrCreateCurrentSeason();
        String seasonSql = "INSERT INTO season_leaderboards (season_id, user_id, total_points, correct_predictions, total_predictions, current_streak, best_streak) " +
            "SELECT :seasonId, user_id, SUM(total_points), SUM(correct_predictions), SUM(total_predictions), 0, 0 " +
            "FROM event_leaderboards " +
            "GROUP BY user_id";
        int seasonRows = entityManager.createNativeQuery(seasonSql)
            .setParameter("seasonId", season.getId())
            .executeUpdate();
        log.info("Inserted {} rows into season_leaderboards for season {}.", seasonRows, season.getName());

        // 4. Distribute Rewards for all events
        List<?> eventIds = entityManager.createNativeQuery("SELECT DISTINCT event_id FROM event_leaderboards").getResultList();
        for (Object idObj : eventIds) {
            Long eventId = ((Number) idObj).longValue();
            try {
                distributeEventRewards(eventId);
            } catch (Exception e) {
                log.error("Failed to distribute rewards for event {}: {}", eventId, e.getMessage());
            }
        }
        log.info("Finished distributing past event rewards.");
    }
}
