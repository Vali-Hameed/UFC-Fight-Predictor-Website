package com.valihameed.ufcfightpredictor.models;

import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;

@Entity
@Table(name = "season_leaderboard", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"season_id", "user_id"})
}, indexes = {
        @Index(name = "idx_season_lb_season_points", columnList = "season_id, total_points DESC")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SeasonLeaderboard {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "season_id", nullable = false)
    private Long seasonId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Builder.Default
    @Column(name = "total_points", nullable = false, columnDefinition = "integer default 0")
    private Integer totalPoints = 0;

    @Builder.Default
    @Column(name = "correct_predictions", nullable = false, columnDefinition = "integer default 0")
    private Integer correctPredictions = 0;

    @Builder.Default
    @Column(name = "total_predictions", nullable = false, columnDefinition = "integer default 0")
    private Integer totalPredictions = 0;

    @Builder.Default
    @Column(name = "current_streak", nullable = false, columnDefinition = "integer default 0")
    private Integer currentStreak = 0;

    @Builder.Default
    @Column(name = "best_streak", nullable = false, columnDefinition = "integer default 0")
    private Integer bestStreak = 0;

    @Column(name = "last_updated")
    private OffsetDateTime lastUpdated;
}
