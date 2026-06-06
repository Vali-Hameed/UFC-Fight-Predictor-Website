package com.valihameed.ufcfightpredictor.repository;

import com.valihameed.ufcfightpredictor.users.role;
import com.valihameed.ufcfightpredictor.users.user;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
public class UserRepositoryTest {

    @Autowired
    private userRepository userRepository;

    @Autowired
    private roleRepository roleRepository;

    private user testUser;

    @BeforeEach
    void setUp() {
        role userRole = new role();
        userRole.setName("ROLE_USER");
        roleRepository.save(userRole);

        testUser = new user(
                "John",
                "Doe",
                "johndoe",
                "john@example.com",
                "password123",
                userRole
        );
        userRepository.save(testUser);
    }

    @Test
    void itShouldFindByUsername() {
        Optional<user> foundUser = userRepository.findByUsername("johndoe");
        
        assertThat(foundUser).isPresent();
        assertThat(foundUser.get().getUsername()).isEqualTo("johndoe");
    }

    @Test
    void itShouldNotFindByUsernameWhenDoesNotExist() {
        Optional<user> foundUser = userRepository.findByUsername("unknown");
        
        assertThat(foundUser).isNotPresent();
    }

    @Test
    void itShouldFindByEmail() {
        Optional<user> foundUser = userRepository.findByEmail("john@example.com");
        
        assertThat(foundUser).isPresent();
        assertThat(foundUser.get().getEmail()).isEqualTo("john@example.com");
    }

    @Autowired
    private org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager entityManager;

    @Test
    void itShouldEnableAppUser() {
        // Given user is currently disabled
        testUser.setEnabled(false);
        userRepository.save(testUser);
        
        // When
        int result = userRepository.enableAppUser("john@example.com");
        entityManager.clear(); // Clear cache to force DB fetch
        
        // Then
        assertThat(result).isEqualTo(1);
        Optional<user> enabledUser = userRepository.findByEmail("john@example.com");
        assertThat(enabledUser.get().isEnabled()).isTrue();
    }
}
