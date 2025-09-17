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
@Table(name = "users") // avoid conflict with SQL reserved word
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder@EqualsAndHashCode
public class user implements UserDetails {
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
    private role role;

    @Column(unique = true, nullable = false)
    private boolean locked;
    @Column(unique = true, nullable = false)
    private boolean enabled;

    public user(String username, String email, String password, String profileImageUrl, role role, boolean locked, boolean enabled) {
        this.username = username;
        this.email = email;
        this.password = password;
        this.profileImageUrl = profileImageUrl;
        this.role = role;
        this.locked = locked;
        this.enabled = enabled;
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