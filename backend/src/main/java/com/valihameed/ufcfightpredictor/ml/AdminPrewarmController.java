package com.valihameed.ufcfightpredictor.ml;

import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/prewarm")
@AllArgsConstructor
public class AdminPrewarmController {
    private final MlPrewarmService mlPrewarmService;
    private final PrewarmConfigService prewarmConfigService;
    private final PrewarmLogRepository prewarmLogRepository;

    @GetMapping("/logs")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<List<PrewarmLog>> listLogs() {
        return ResponseEntity.ok(prewarmLogRepository.findAll());
    }

    @PostMapping("/trigger")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<PrewarmLog> triggerNow() {
        PrewarmLog log = mlPrewarmService.runPrewarmManual();
        return ResponseEntity.ok(log);
    }

    @PostMapping("/toggle")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<?> toggle(@RequestParam boolean enabled) {
        prewarmConfigService.setEnabled(enabled);
        return ResponseEntity.ok().body(java.util.Map.of("prewarmEnabled", prewarmConfigService.isEnabled()));
    }
}
