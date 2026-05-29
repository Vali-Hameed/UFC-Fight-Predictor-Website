package com.valihameed.ufcfightpredictor.ml;

import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;

@Entity
@Table(name = "prewarm_logs")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PrewarmLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "started_at")
    private OffsetDateTime startedAt;

    @Column(name = "completed_at")
    private OffsetDateTime completedAt;

    @Column(name = "events_found")
    private Integer eventsFound;

    @Column(name = "fights_processed")
    private Integer fightsProcessed;

    @Column(name = "success_count")
    private Integer successCount;

    @Column(name = "failure_count")
    private Integer failureCount;

    private String status; // STARTED, COMPLETED, FAILED

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;
}
