package com.valihameed.ufcfightpredictor.ml;

import com.valihameed.ufcfightpredictor.models.Event;
import com.valihameed.ufcfightpredictor.models.Fight;
import com.valihameed.ufcfightpredictor.repository.EventRepository;
import com.valihameed.ufcfightpredictor.repository.FightRepository;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.List;

@Component
@AllArgsConstructor
public class MlPrewarmService {
    private static final Logger log = LoggerFactory.getLogger(MlPrewarmService.class);

    private final EventRepository eventRepository;
    private final FightRepository fightRepository;
    private final MlService mlService;
    private final PrewarmLogRepository prewarmLogRepository;
    private final PrewarmConfigService prewarmConfigService;

    @Scheduled(cron = "${prewarm.cron:0 0 * * * *}")
    public void runPrewarm() {
        if (!prewarmConfigService.isEnabled()) {
            log.info("ML prewarm disabled");
            return;
        }
        runPrewarmInternal();
    }

    // public method to allow manual trigger
    public PrewarmLog runPrewarmManual() {
        if (!prewarmConfigService.isEnabled()) {
            log.info("ML prewarm disabled (manual trigger ignored)");
            PrewarmLog disabledLog = PrewarmLog.builder().startedAt(OffsetDateTime.now()).completedAt(OffsetDateTime.now()).eventsFound(0).fightsProcessed(0).successCount(0).failureCount(0).status("SKIPPED").build();
            prewarmLogRepository.save(disabledLog);
            return disabledLog;
        }
        return runPrewarmInternal();
    }

    private PrewarmLog runPrewarmInternal() {
        long lookaheadHours = prewarmConfigService.getLookaheadHours();
        log.info("Starting ML pre-warm job (lookahead {} hours)", lookaheadHours);
        PrewarmLog logEntry = PrewarmLog.builder().startedAt(OffsetDateTime.now()).status("STARTED").eventsFound(0).fightsProcessed(0).successCount(0).failureCount(0).build();
        prewarmLogRepository.save(logEntry);
        int totalEvents = 0;
        int totalFights = 0;
        int successes = 0;
        int failures = 0;
        try {
            OffsetDateTime now = OffsetDateTime.now();
            OffsetDateTime end = now.plusHours(lookaheadHours);
            List<Event> events = eventRepository.findByEventDateBetweenAndStatus(now, end, "UPCOMING");
            totalEvents = events.size();
            for (Event e : events) {
                log.info("Pre-warming ML for event {} (id={})", e.getName(), e.getId());
                List<Fight> fights = fightRepository.findByEventId(e.getId());
                for (Fight f : fights) {
                    try {
                        if (f.getFighter1Name() == null || f.getFighter2Name() == null) continue;
                        totalFights++;
                        var p = mlService.forceRefreshPrediction(f.getFighter1Name(), f.getFighter2Name(), f.getId());
                        if (p != null) successes++; else failures++;
                        Thread.sleep(200); // small throttle to avoid burst
                    } catch (Exception ex) {
                        failures++;
                        log.warn("Failed to prewarm ML for fight {}: {}", f.getId(), ex.getMessage());
                    }
                }
            }
            logEntry.setStatus("COMPLETED");
        } catch (Exception ex) {
            log.error("Prewarm job failed: {}", ex.getMessage(), ex);
            logEntry.setStatus("FAILED");
            logEntry.setErrorMessage(ex.getMessage());
        } finally {
            logEntry.setCompletedAt(OffsetDateTime.now());
            logEntry.setEventsFound(totalEvents);
            logEntry.setFightsProcessed(totalFights);
            logEntry.setSuccessCount(successes);
            logEntry.setFailureCount(failures);
            prewarmLogRepository.save(logEntry);
            log.info("ML pre-warm job completed: events={}, fights={}, success={}, failures={}", totalEvents, totalFights, successes, failures);
        }
        return logEntry;
    }
}
