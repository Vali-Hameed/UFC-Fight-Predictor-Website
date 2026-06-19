package com.valihameed.ufcfightpredictor.models;

import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;

@Entity
@Table(name = "event_leaderboard", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"event_id", "user_id"})
}, indexes = {
        @Index(name = "idx_event_lb_event_points", columnList = "event_id, total_points DESC")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EventLeaderboard {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_id", nullable = false)
    private Long eventId;

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

    @Column(name = "last_updated")
    private OffsetDateTime lastUpdated;
}
