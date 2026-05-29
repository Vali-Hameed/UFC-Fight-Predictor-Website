package com.valihameed.ufcfightpredictor.models;

import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;

@Entity
@Table(name = "leaderboard")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Leaderboard {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", unique = true)
    private Long userId;

    @Column(name = "total_points")
    private Integer totalPoints;

    @Column(name = "correct_predictions")
    private Integer correctPredictions;

    @Column(name = "total_predictions")
    private Integer totalPredictions;

    @Column(name = "current_streak")
    private Integer currentStreak;

    @Column(name = "best_streak")
    private Integer bestStreak;

    @Column(name = "last_updated")
    private OffsetDateTime lastUpdated;
}
