package com.valihameed.ufcfightpredictor.scraper;

import com.valihameed.ufcfightpredictor.models.ScrapeLog;
import com.valihameed.ufcfightpredictor.ml.PrewarmConfigService;
import com.valihameed.ufcfightpredictor.repository.ScrapeLogRepository;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/v1/internal/scraper/logs")
@AllArgsConstructor
public class ScrapeLogController {
    private final ScrapeLogRepository scrapeLogRepository;

    @PostMapping
    public ResponseEntity<ScrapeLog> ingest(@RequestBody ScrapeLogRequest request) {
        ScrapeLog log = ScrapeLog.builder()
            .startedAt(request.getStartedAt())
            .completedAt(request.getCompletedAt())
            .eventsFound(request.getEventsFound())
            .fightsUpdated(request.getFightsUpdated())
            .status(request.getStatus())
            .errorMessage(request.getErrorMessage())
            .build();
        return ResponseEntity.ok(scrapeLogRepository.save(log));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public List<ScrapeLog> list() {
        return scrapeLogRepository.findAll();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        scrapeLogRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    @Data
    public static class ScrapeLogRequest {
        private OffsetDateTime startedAt;
        private OffsetDateTime completedAt;
        private Integer eventsFound;
        private Integer fightsUpdated;
        private String status;
        private String errorMessage;
    }
}