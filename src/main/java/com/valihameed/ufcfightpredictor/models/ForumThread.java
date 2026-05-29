package com.valihameed.ufcfightpredictor.models;

import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;

@Entity
@Table(name = "forum_threads")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ForumThread {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_id")
    private Long eventId;

    @Column(name = "fight_id")
    private Long fightId;

    @Column(name = "created_by")
    private Long createdBy;

    private String title;

    @Column(name = "created_at")
    private OffsetDateTime createdAt;
}
