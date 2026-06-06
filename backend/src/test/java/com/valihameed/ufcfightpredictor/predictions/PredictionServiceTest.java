package com.valihameed.ufcfightpredictor.predictions;

import com.valihameed.ufcfightpredictor.models.UserPrediction;
import com.valihameed.ufcfightpredictor.repository.CommunityVoteRepository;
import com.valihameed.ufcfightpredictor.repository.FightRepository;
import com.valihameed.ufcfightpredictor.repository.UserPredictionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
public class PredictionServiceTest {

    @Mock private UserPredictionRepository userPredictionRepository;
    @Mock private FightRepository fightRepository;
    @Mock private CommunityVoteRepository communityVoteRepository;
    @Mock private PredictionTransactionalHelper txHelper;

    private PredictionService underTest;

    @BeforeEach
    void setUp() {
        underTest = new PredictionService(
                userPredictionRepository,
                fightRepository,
                communityVoteRepository,
                txHelper
        );
    }

    @Test
    void submitPredictionRejectsConcurrentRequests() throws InterruptedException {
        // Given
        Long userId = 1L;
        PredictionRequest req = new PredictionRequest(10L, "Fighter A", "DEC", null, false);
        
        // Mock txHelper to sleep to simulate a long DB transaction
        given(txHelper.doSubmitPrediction(anyLong(), any(PredictionRequest.class))).willAnswer(invocation -> {
            Thread.sleep(500);
            return new UserPrediction();
        });

        int numThreads = 5;
        ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        CountDownLatch latch = new CountDownLatch(numThreads);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);

        // When - fire 5 requests simultaneously for the same user+fight
        for (int i = 0; i < numThreads; i++) {
            executor.submit(() -> {
                try {
                    underTest.submitPrediction(userId, req);
                    successCount.incrementAndGet();
                } catch (IllegalStateException e) {
                    if (e.getMessage().contains("Prediction already being processed")) {
                        failureCount.incrementAndGet();
                    }
                } finally {
                    latch.countDown();
                }
            });
        }
        latch.await();

        // Then - exactly 1 request should succeed and 4 should fail fast
        assertThat(successCount.get()).isEqualTo(1);
        assertThat(failureCount.get()).isEqualTo(4);
    }

    @Test
    void submitPredictionSucceedsSequentially() {
        Long userId = 1L;
        PredictionRequest req = new PredictionRequest(10L, "Fighter A", "DEC", null, false);
        
        given(txHelper.doSubmitPrediction(anyLong(), any(PredictionRequest.class))).willReturn(new UserPrediction());

        underTest.submitPrediction(userId, req);
        
        // Second call should also succeed because the lock is cleared in the finally block
        UserPrediction result = underTest.submitPrediction(userId, req);
        assertThat(result).isNotNull();
    }
}
