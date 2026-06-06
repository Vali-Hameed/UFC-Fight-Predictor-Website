package com.valihameed.ufcfightpredictor.repository;

import com.valihameed.ufcfightpredictor.models.PredictionResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
public class PredictionResultRepositoryTest {

    @Autowired
    private PredictionResultRepository predictionResultRepository;

    @BeforeEach
    void setUp() {
        PredictionResult result1 = PredictionResult.builder()
                .userPredictionId(10L)
                .isWinnerCorrect(true)
                .isMethodCorrect(false)
                .isRoundCorrect(false)
                .pointsAwarded(10)
                .build();

        PredictionResult result2 = PredictionResult.builder()
                .userPredictionId(10L)
                .isWinnerCorrect(true)
                .isMethodCorrect(true)
                .isRoundCorrect(true)
                .pointsAwarded(25)
                .build();

        predictionResultRepository.saveAll(List.of(result1, result2));
    }

    @Test
    void itShouldFindByUserPredictionId() {
        List<PredictionResult> results = predictionResultRepository.findByUserPredictionId(10L);

        assertThat(results).hasSize(2);
        assertThat(results).extracting(PredictionResult::getPointsAwarded).containsExactlyInAnyOrder(10, 25);
    }
}
