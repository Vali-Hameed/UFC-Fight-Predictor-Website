package com.valihameed.ufcfightpredictor.predictions;

import com.valihameed.ufcfightpredictor.models.CommunityVote;
import com.valihameed.ufcfightpredictor.models.Event;
import com.valihameed.ufcfightpredictor.models.Fight;
import com.valihameed.ufcfightpredictor.models.UserPrediction;
import com.valihameed.ufcfightpredictor.repository.CommunityVoteRepository;
import com.valihameed.ufcfightpredictor.repository.EventRepository;
import com.valihameed.ufcfightpredictor.repository.FightRepository;
import com.valihameed.ufcfightpredictor.repository.UserPredictionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class PredictionTransactionalHelperTest {

    @Mock private UserPredictionRepository userPredictionRepository;
    @Mock private FightRepository fightRepository;
    @Mock private CommunityVoteRepository communityVoteRepository;
    @Mock private EventRepository eventRepository;

    private PredictionTransactionalHelper underTest;

    @BeforeEach
    void setUp() {
        underTest = new PredictionTransactionalHelper(
                userPredictionRepository,
                fightRepository,
                communityVoteRepository,
                eventRepository
        );
    }

    @Test
    void canSubmitNewPrediction() {
        // Given
        Long userId = 1L;
        PredictionRequest req = new PredictionRequest(10L, "Conor McGregor", "KO/TKO", 2, false);

        Fight fight = new Fight();
        fight.setId(10L);
        fight.setEventId(100L);
        fight.setStatus("UPCOMING");
        fight.setFighter1Name("Conor McGregor");
        fight.setFighter2Name("Dustin Poirier");

        Event event = new Event();
        event.setId(100L);
        event.setEventDate(OffsetDateTime.now().plusDays(5));

        given(fightRepository.findById(10L)).willReturn(Optional.of(fight));
        given(eventRepository.findById(100L)).willReturn(Optional.of(event));
        given(userPredictionRepository.findFirstByUserIdAndFightId(userId, 10L)).willReturn(Optional.empty());
        given(communityVoteRepository.findByFightId(10L)).willReturn(Optional.empty());
        
        UserPrediction savedPrediction = new UserPrediction();
        given(userPredictionRepository.save(any(UserPrediction.class))).willReturn(savedPrediction);

        // When
        underTest.doSubmitPrediction(userId, req);

        // Then
        ArgumentCaptor<UserPrediction> predictionCaptor = ArgumentCaptor.forClass(UserPrediction.class);
        verify(userPredictionRepository).save(predictionCaptor.capture());
        UserPrediction captured = predictionCaptor.getValue();

        assertThat(captured.getPredictedWinner()).isEqualTo("Conor McGregor");
        assertThat(captured.getUserId()).isEqualTo(userId);
        assertThat(captured.getFightId()).isEqualTo(10L);

        ArgumentCaptor<CommunityVote> voteCaptor = ArgumentCaptor.forClass(CommunityVote.class);
        verify(communityVoteRepository).save(voteCaptor.capture());
        CommunityVote capturedVote = voteCaptor.getValue();
        assertThat(capturedVote.getFighter1Votes()).isEqualTo(1); // Added 1 to Conor
    }

    @Test
    void willThrowWhenFightIsCompleted() {
        // Given
        PredictionRequest req = new PredictionRequest(10L, "Conor McGregor", "KO/TKO", 2, false);
        Fight fight = new Fight();
        fight.setId(10L);
        fight.setStatus("COMPLETED");

        given(fightRepository.findById(10L)).willReturn(Optional.of(fight));

        // When / Then
        assertThatThrownBy(() -> underTest.doSubmitPrediction(1L, req))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Fight is locked for predictions");
    }

    @Test
    void willThrowWhenEventAlreadyStarted() {
        // Given
        PredictionRequest req = new PredictionRequest(10L, "Conor McGregor", "KO/TKO", 2, false);
        Fight fight = new Fight();
        fight.setId(10L);
        fight.setEventId(100L);
        fight.setStatus("UPCOMING");

        Event event = new Event();
        event.setId(100L);
        event.setEventDate(OffsetDateTime.now().minusHours(1)); // Started 1 hr ago

        given(fightRepository.findById(10L)).willReturn(Optional.of(fight));
        given(eventRepository.findById(100L)).willReturn(Optional.of(event));

        // When / Then
        assertThatThrownBy(() -> underTest.doSubmitPrediction(1L, req))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Event has already started. Predictions are locked.");
    }
}
