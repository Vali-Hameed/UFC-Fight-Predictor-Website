package com.valihameed.ufcfightpredictor.models;

import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;

@Entity
@Table(name = "ml_predictions")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MlPrediction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "fight_id", unique = true)
    private Long fightId;

    @Column(name = "predicted_winner")
    private String predictedWinner;

    @Column(name = "confidence_score")
    private Double confidenceScore;

    @Column(name = "cached_at")
    private OffsetDateTime cachedAt;
}
