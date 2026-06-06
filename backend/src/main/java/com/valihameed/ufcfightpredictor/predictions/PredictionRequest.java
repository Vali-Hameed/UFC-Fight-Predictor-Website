package com.valihameed.ufcfightpredictor.predictions;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@lombok.AllArgsConstructor
@lombok.NoArgsConstructor
public class PredictionRequest {
    @NotNull
    private Long fightId;

    @NotBlank
    private String predictedWinner;

    @NotBlank
    private String predictedMethod;

    @NotNull
    @Min(0)
    private Integer predictedRound;
}
