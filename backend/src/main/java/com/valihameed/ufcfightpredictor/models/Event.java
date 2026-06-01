package com.valihameed.ufcfightpredictor.models;

import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;

@Entity
@Table(name = "events", indexes = {
        @Index(name = "idx_event_date_status", columnList = "event_date, status")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Event {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    @Column(name = "event_date")
    private OffsetDateTime eventDate;

    private String location;

    private String status;

    @Column(name = "scraped_at")
    private OffsetDateTime scrapedAt;
}
