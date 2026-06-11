package com.valihameed.ufcfightpredictor.users;

import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;

@Entity
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@Table(name = "username_history")
public class UsernameHistory {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private user user;

    @Column(name = "previous_username", nullable = false)
    private String previousUsername;

    @Column(name = "changed_at", nullable = false)
    private OffsetDateTime changedAt;
}
