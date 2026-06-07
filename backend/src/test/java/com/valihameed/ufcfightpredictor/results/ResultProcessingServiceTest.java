package com.valihameed.ufcfightpredictor.results;

import com.valihameed.ufcfightpredictor.models.*;
import com.valihameed.ufcfightpredictor.repository.*;
import com.valihameed.ufcfightpredictor.repository.EventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ResultProcessingServiceTest {

    @Mock private FightRepository fightRepository;
    @Mock private UserPredictionRepository userPredictionRepository;
    @Mock private PredictionResultRepository predictionResultRepository;
    @Mock private LeaderboardRepository leaderboardRepository;
    @Mock private NotificationRepository notificationRepository;
    @Mock private com.valihameed.ufcfightpredictor.notifications.NotificationService notificationService;
    @Mock private EventRepository eventRepository;

    private ResultProcessingService underTest;

    @BeforeEach
    void setUp() {
        underTest = new ResultProcessingService(
                fightRepository,
                userPredictionRepository,
                predictionResultRepository,
                leaderboardRepository,
                notificationRepository,
                notificationService,
                eventRepository
        );
    }

    @Test
    void processFightResultPerfectPrediction() {
        // Given
        Long fightId = 1L;
        Fight fight = new Fight();
        fight.setId(fightId);
        fight.setResultWinner("Conor McGregor");
        fight.setResultMethod("KO/TKO");
        fight.setResultRound(2);

        UserPrediction prediction = new UserPrediction();
        prediction.setId(10L);
        prediction.setUserId(100L);
        prediction.setPredictedWinner("Conor McGregor");
        prediction.setPredictedMethod("KO/TKO");
        prediction.setPredictedRound(2);

        Leaderboard leaderboard = Leaderboard.builder()
                .userId(100L)
                .totalPoints(0)
                .totalPredictions(0)
                .correctPredictions(0)
                .currentStreak(0)
                .bestStreak(0)
                .build();

        given(fightRepository.findById(fightId)).willReturn(Optional.of(fight));
        given(userPredictionRepository.findByFightId(fightId)).willReturn(List.of(prediction));
        given(predictionResultRepository.findByUserPredictionId(10L)).willReturn(Collections.emptyList());
        given(leaderboardRepository.findByUserId(100L)).willReturn(Optional.of(leaderboard));

        // When
        underTest.processFightResult(fightId);

        // Then
        ArgumentCaptor<PredictionResult> resultCaptor = ArgumentCaptor.forClass(PredictionResult.class);
        verify(predictionResultRepository).save(resultCaptor.capture());
        PredictionResult savedResult = resultCaptor.getValue();
        
        assertThat(savedResult.getIsWinnerCorrect()).isTrue();
        assertThat(savedResult.getIsMethodCorrect()).isTrue();
        assertThat(savedResult.getIsRoundCorrect()).isTrue();
        assertThat(savedResult.getPointsAwarded()).isEqualTo(10 + 4 + 7 + 10); // 31 points

        ArgumentCaptor<Leaderboard> lbCaptor = ArgumentCaptor.forClass(Leaderboard.class);
        verify(leaderboardRepository).save(lbCaptor.capture());
        Leaderboard savedLb = lbCaptor.getValue();
        
        assertThat(savedLb.getTotalPoints()).isEqualTo(31);
        assertThat(savedLb.getCorrectPredictions()).isEqualTo(1);
        assertThat(savedLb.getCurrentStreak()).isEqualTo(1);
    }

    @Test
    void processFightResultIncorrectPrediction() {
        // Given
        Long fightId = 1L;
        Fight fight = new Fight();
        fight.setId(fightId);
        fight.setResultWinner("Conor McGregor");
        fight.setResultMethod("KO/TKO");
        fight.setResultRound(2);

        UserPrediction prediction = new UserPrediction();
        prediction.setId(10L);
        prediction.setUserId(100L);
        prediction.setPredictedWinner("Dustin Poirier");
        prediction.setPredictedMethod("DEC");
        prediction.setPredictedRound(3);

        Leaderboard leaderboard = Leaderboard.builder()
                .userId(100L)
                .totalPoints(50)
                .totalPredictions(5)
                .correctPredictions(2)
                .currentStreak(2)
                .bestStreak(2)
                .build();

        given(fightRepository.findById(fightId)).willReturn(Optional.of(fight));
        given(userPredictionRepository.findByFightId(fightId)).willReturn(List.of(prediction));
        given(predictionResultRepository.findByUserPredictionId(10L)).willReturn(Collections.emptyList());
        given(leaderboardRepository.findByUserId(100L)).willReturn(Optional.of(leaderboard));

        // When
        underTest.processFightResult(fightId);

        // Then
        ArgumentCaptor<PredictionResult> resultCaptor = ArgumentCaptor.forClass(PredictionResult.class);
        verify(predictionResultRepository).save(resultCaptor.capture());
        PredictionResult savedResult = resultCaptor.getValue();
        
        assertThat(savedResult.getIsWinnerCorrect()).isFalse();
        assertThat(savedResult.getPointsAwarded()).isEqualTo(0);

        ArgumentCaptor<Leaderboard> lbCaptor = ArgumentCaptor.forClass(Leaderboard.class);
        verify(leaderboardRepository).save(lbCaptor.capture());
        Leaderboard savedLb = lbCaptor.getValue();
        
        assertThat(savedLb.getTotalPoints()).isEqualTo(50);
        assertThat(savedLb.getTotalPredictions()).isEqualTo(6);
        assertThat(savedLb.getCurrentStreak()).isEqualTo(0); // Streak reset
    }

    @Test
    void willThrowWhenFightResultNotSet() {
        // Given
        Fight fight = new Fight();
        fight.setId(1L);
        // Result Winner is null

        given(fightRepository.findById(1L)).willReturn(Optional.of(fight));

        // When / Then
        assertThatThrownBy(() -> underTest.processFightResult(1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Fight result not set");
    }

    @Test
    void processFightResultSkipsNotificationWhenOptedOut() {
        // Given
        Long fightId = 1L;
        Fight fight = new Fight();
        fight.setId(fightId);
        fight.setResultWinner("Conor McGregor");
        fight.setResultMethod("KO/TKO");
        fight.setResultRound(2);

        UserPrediction prediction = new UserPrediction();
        prediction.setId(10L);
        prediction.setUserId(100L);
        prediction.setPredictedWinner("Conor McGregor");
        prediction.setPredictedMethod("KO/TKO");
        prediction.setPredictedRound(2);
        prediction.setOptOutResultNotification(true);

        Leaderboard leaderboard = Leaderboard.builder()
                .userId(100L)
                .totalPoints(0)
                .build();

        given(fightRepository.findById(fightId)).willReturn(Optional.of(fight));
        given(userPredictionRepository.findByFightId(fightId)).willReturn(List.of(prediction));
        given(predictionResultRepository.findByUserPredictionId(10L)).willReturn(Collections.emptyList());
        given(leaderboardRepository.findByUserId(100L)).willReturn(Optional.of(leaderboard));

        // When
        underTest.processFightResult(fightId);

        // Then
        verify(notificationService, never()).createNotification(any());
    }
}
