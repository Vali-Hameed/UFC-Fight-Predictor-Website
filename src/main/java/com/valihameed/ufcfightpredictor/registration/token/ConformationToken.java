package com.valihameed.ufcfightpredictor.registration.token;

import com.valihameed.ufcfightpredictor.users.user;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
@Getter@Setter@NoArgsConstructor
public class ConformationToken {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false)
    private String token;
    @Column(nullable = false)
    private LocalDateTime createdAt;
    @Column(nullable = false)
    private LocalDateTime expiresAt;
    private LocalDateTime confirmedAt;
    @ManyToOne
    @JoinColumn(nullable = false,
    name = "user_id")
    private user user;

    public ConformationToken(String token, LocalDateTime createdAt, LocalDateTime expiresAt, LocalDateTime confirmedAt,user user) {
        this.token = token;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
        this.confirmedAt = confirmedAt;
        this.user = user;
    }
}
