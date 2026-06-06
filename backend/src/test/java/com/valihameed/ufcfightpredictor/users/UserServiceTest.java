package com.valihameed.ufcfightpredictor.users;

import com.valihameed.ufcfightpredictor.registration.token.ConfirmationToken;
import com.valihameed.ufcfightpredictor.registration.token.ConfirmationTokenService;
import com.valihameed.ufcfightpredictor.repository.userRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class UserServiceTest {

    @Mock private userRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private BCryptPasswordEncoder bCryptPasswordEncoder;
    @Mock private ConfirmationTokenService confirmationTokenService;

    private userService underTest;
    private user testUser;

    @BeforeEach
    void setUp() {
        underTest = new userService(
                userRepository,
                passwordEncoder,
                bCryptPasswordEncoder,
                confirmationTokenService
        );

        role userRole = new role();
        userRole.setName("ROLE_USER");

        testUser = new user(
                "John",
                "Doe",
                "johndoe",
                "john@example.com",
                "password123",
                userRole
        );
    }

    @Test
    void canCreateNewUser() {
        // Given
        role userRole = new role();
        userRole.setName("ROLE_USER");

        given(userRepository.findByUsername(anyString())).willReturn(Optional.empty());
        given(userRepository.findByEmail(anyString())).willReturn(Optional.empty());
        given(passwordEncoder.encode(anyString())).willReturn("encoded_password");

        // When
        underTest.createNewUser("johndoe", "john@example.com", "password", userRole);

        // Then
        ArgumentCaptor<user> userArgumentCaptor = ArgumentCaptor.forClass(user.class);
        verify(userRepository).save(userArgumentCaptor.capture());

        user capturedUser = userArgumentCaptor.getValue();
        assertThat(capturedUser.getUsername()).isEqualTo("johndoe");
        assertThat(capturedUser.getEmail()).isEqualTo("john@example.com");
        assertThat(capturedUser.getPassword()).isEqualTo("encoded_password");
    }

    @Test
    void willThrowWhenUsernameIsTaken() {
        // Given
        given(userRepository.findByUsername(anyString())).willReturn(Optional.of(testUser));
        role userRole = new role();

        // When / Then
        assertThatThrownBy(() -> underTest.createNewUser("johndoe", "new@example.com", "password", userRole))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Username already taken");
    }

    @Test
    void canLoadUserByUsernameUsingEmail() {
        // Given
        given(userRepository.findByEmail(anyString())).willReturn(Optional.of(testUser));

        // When
        UserDetails userDetails = underTest.loadUserByUsername("john@example.com");

        // Then
        assertThat(userDetails.getUsername()).isEqualTo("johndoe");
    }

    @Test
    void canLoadUserByUsernameUsingUsername() {
        // Given
        given(userRepository.findByUsername(anyString())).willReturn(Optional.of(testUser));

        // When
        UserDetails userDetails = underTest.loadUserByUsername("johndoe");

        // Then
        assertThat(userDetails.getUsername()).isEqualTo("johndoe");
    }

    @Test
    void willThrowWhenUserNotFoundByUsernameOrEmail() {
        // Given
        given(userRepository.findByUsername(anyString())).willReturn(Optional.empty());

        // When / Then
        assertThatThrownBy(() -> underTest.loadUserByUsername("unknownuser"))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessageContaining("user with unknownuser not found");
    }

    @Test
    void canSignUpUser() {
        // Given
        given(userRepository.findByEmail(anyString())).willReturn(Optional.empty());
        given(userRepository.findByUsername(anyString())).willReturn(Optional.empty());
        given(bCryptPasswordEncoder.encode(anyString())).willReturn("encoded_password");

        // When
        String token = underTest.signUpUser(testUser);

        // Then
        assertThat(token).isNotBlank();
        
        ArgumentCaptor<user> userArgumentCaptor = ArgumentCaptor.forClass(user.class);
        verify(userRepository).save(userArgumentCaptor.capture());
        assertThat(userArgumentCaptor.getValue().getPassword()).isEqualTo("encoded_password");

        ArgumentCaptor<ConfirmationToken> tokenArgumentCaptor = ArgumentCaptor.forClass(ConfirmationToken.class);
        verify(confirmationTokenService).saveConformationToken(tokenArgumentCaptor.capture());
        assertThat(tokenArgumentCaptor.getValue().getToken()).isEqualTo(token);
    }
}
