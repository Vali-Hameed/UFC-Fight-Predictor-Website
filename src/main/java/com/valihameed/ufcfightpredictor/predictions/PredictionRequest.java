package com.valihameed.ufcfightpredictor.predictions;

import lombok.Data;

@Data
public class PredictionRequest {
    private Long fightId;
    private String predictedWinner;
    private String predictedMethod;
    private Integer predictedRound;
}
