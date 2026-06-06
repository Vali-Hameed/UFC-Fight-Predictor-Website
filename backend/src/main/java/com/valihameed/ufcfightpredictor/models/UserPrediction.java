package com.valihameed.ufcfightpredictor.models;

import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;

@Entity
@Table(name = "user_predictions", indexes = {
        @Index(name = "idx_user_prediction_user_fight", columnList = "user_id, fight_id")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserPrediction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "fight_id")
    private Long fightId;

    @Column(name = "predicted_winner")
    private String predictedWinner;

    @Column(name = "predicted_method")
    private String predictedMethod;

    @Column(name = "predicted_round")
    private Integer predictedRound;

    @Column(name = "submitted_at")
    private OffsetDateTime submittedAt;

    private Boolean locked;

    @Column(name = "opt_out_result_notification", nullable = false, columnDefinition = "boolean default false")
    private Boolean optOutResultNotification = false;
}
