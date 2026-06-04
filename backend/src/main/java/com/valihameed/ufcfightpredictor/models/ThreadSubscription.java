package com.valihameed.ufcfightpredictor.models;

import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;

@Entity
@Table(name = "thread_subscriptions", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"user_id", "thread_id"})
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ThreadSubscription {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "thread_id", nullable = false)
    private Long threadId;

    @Column(name = "created_at")
    private OffsetDateTime createdAt;
}
