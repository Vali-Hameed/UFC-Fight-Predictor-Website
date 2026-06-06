package com.valihameed.ufcfightpredictor.users;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections;
import java.util.List;


@Entity
// avoid conflict with SQL reserved word
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder@EqualsAndHashCode
@Table(name = "users")
public class user implements UserDetails {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column
    private String firstName;
    @Column
    private String lastName;


    @Column(unique = true, nullable = false)
    private String username;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(nullable = false)
    private String password;  // store BCrypt hash

    private String profileImageUrl;  // S3 URL

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "role_id")
    private role role;

    @Column(nullable = false)
    private boolean locked;
    @Column(nullable = false)
    private boolean enabled;
    
    @Builder.Default
    @Column(nullable = false, columnDefinition = "boolean default true")
    private boolean publicProfile = true;

    @Builder.Default
    @Column(nullable = false, columnDefinition = "integer default 0")
    private Integer tokenVersion = 0;

    @Builder.Default
    @Column(nullable = false, columnDefinition = "integer default 0")
    private Integer warningCount = 0;

    @Column
    private java.time.OffsetDateTime bannedFromForumUntil;

    @Builder.Default
    @Column(name = "opt_out_email_notifications", nullable = false, columnDefinition = "boolean default false")
    private boolean optOutEmailNotifications = false;

    public user(String firstName, String lastName, String username, String email, String password,role role) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.username = username;
        this.email = email;
        this.password = password;
        this.role = role;
        this.locked = false;
        this.enabled = false;
        this.publicProfile = true;
        this.tokenVersion = 0;
        this.warningCount = 0;
        this.optOutEmailNotifications = false;
    }

    public user(String firstName, String lastName, String username, String email, String password, String profileImageUrl, role role, boolean locked, boolean enabled) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.username = username;
        this.email = email;
        this.password = password;
        this.profileImageUrl = profileImageUrl;
        this.role = role;
        this.locked = locked;
        this.enabled = enabled;
        this.publicProfile = true;
        this.tokenVersion = 0;
        this.warningCount = 0;
        this.optOutEmailNotifications = false;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        SimpleGrantedAuthority simpleGrantedAuthority = new SimpleGrantedAuthority( role.getName());
        return Collections.singletonList( simpleGrantedAuthority);
    }
    @Override
    public String getPassword() {
        return password;
    }
    @Override
    public String getUsername() {
        return username;
    }
    @Override
    public boolean isAccountNonExpired() {
        return true;
    }
    @Override
    public boolean isAccountNonLocked() {
        return !locked;
    }
    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }
    @Override
    public boolean isEnabled() {
        return enabled;
    }
}