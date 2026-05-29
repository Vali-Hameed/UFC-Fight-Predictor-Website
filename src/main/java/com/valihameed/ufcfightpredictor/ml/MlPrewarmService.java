package com.valihameed.ufcfightpredictor.ml;

import com.valihameed.ufcfightpredictor.models.Event;
import com.valihameed.ufcfightpredictor.models.Fight;
import com.valihameed.ufcfightpredictor.repository.EventRepository;
import com.valihameed.ufcfightpredictor.repository.FightRepository;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
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

    @Value("${prewarm.enabled:true}")
    private boolean enabled;

    @Value("${prewarm.lookahead-hours:3}")
    private long lookaheadHours;

    @Value("${prewarm.cron:0 0 * * * *}")
    private String cron;

    @Scheduled(cron = "${prewarm.cron:0 0 * * * *}")
    public void runPrewarm() {
        if (!enabled) {
            log.info("ML prewarm disabled");
            return;
        }
        log.info("Starting ML pre-warm job (lookahead {} hours)", lookaheadHours);
        OffsetDateTime now = OffsetDateTime.now();
        OffsetDateTime end = now.plusHours(lookaheadHours);
        List<Event> events = eventRepository.findByEventDateBetweenAndStatus(now, end, "UPCOMING");
        for (Event e : events) {
            log.info("Pre-warming ML for event {} (id={})", e.getName(), e.getId());
            List<Fight> fights = fightRepository.findByEventId(e.getId());
            for (Fight f : fights) {
                try {
                    if (f.getFighter1Name() == null || f.getFighter2Name() == null) continue;
                    mlService.forceRefreshPrediction(f.getFighter1Name(), f.getFighter2Name(), f.getId());
                    Thread.sleep(200); // small throttle to avoid burst
                } catch (Exception ex) {
                    log.warn("Failed to prewarm ML for fight {}: {}", f.getId(), ex.getMessage());
                }
            }
        }
        log.info("ML pre-warm job completed");
    }
}
