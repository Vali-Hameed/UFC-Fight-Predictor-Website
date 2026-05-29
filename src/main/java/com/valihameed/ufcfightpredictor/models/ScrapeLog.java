package com.valihameed.ufcfightpredictor.models;

import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;

@Entity
@Table(name = "scrape_logs")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ScrapeLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "started_at")
    private OffsetDateTime startedAt;

    @Column(name = "completed_at")
    private OffsetDateTime completedAt;

    @Column(name = "events_found")
    private Integer eventsFound;

    @Column(name = "fights_updated")
    private Integer fightsUpdated;

    private String status;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;
}
