package com.valihameed.ufcfightpredictor.models;

import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;

@Entity
@Table(name = "user_badges", indexes = {
        @Index(name = "idx_user_badge_user_id", columnList = "user_id")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserBadge {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    /**
     * Badge type enum values:
     * SEASON_CHAMPION    - Won the overall season (annual champion)
     * SEASON_TOP_3       - Finished top 3 in a season
     * EVENT_WINNER       - Won a specific event
     * PERFECT_EVENT      - Got every prediction right for an event
     * STREAK_10          - Achieved a 10+ win streak
     * STREAK_25          - Achieved a 25+ win streak
     */
    @Column(name = "badge_type", nullable = false)
    private String badgeType;

    @Column(name = "badge_label")
    private String badgeLabel; // e.g. "SS25 Champion", "UFC 305 Winner"

    @Column(name = "season_id")
    private Long seasonId;

    @Column(name = "event_id")
    private Long eventId;

    @Column(name = "awarded_at")
    private OffsetDateTime awardedAt;
}
