package com.valihameed.ufcfightpredictor.users;
import jakarta.persistence.*;
import lombok.*;


@Entity
@Table(name = "users") // avoid conflict with SQL reserved word
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String username;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(nullable = false)
    private String password;  // store BCrypt hash

    private String profileImageUrl;  // S3 URL

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "role_id")
    private Role role;
}