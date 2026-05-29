package com.valihameed.ufcfightpredictor.models;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "prediction_results")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PredictionResult {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_prediction_id")
    private Long userPredictionId;

    @Column(name = "is_winner_correct")
    private Boolean isWinnerCorrect;

    @Column(name = "is_method_correct")
    private Boolean isMethodCorrect;

    @Column(name = "is_round_correct")
    private Boolean isRoundCorrect;

    @Column(name = "points_awarded")
    private Integer pointsAwarded;
}
