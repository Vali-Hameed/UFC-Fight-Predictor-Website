package com.valihameed.ufcfightpredictor.registration;

import com.valihameed.ufcfightpredictor.email.EmailSender;
import com.valihameed.ufcfightpredictor.registration.token.ConfirmationToken;
import com.valihameed.ufcfightpredictor.registration.token.ConfirmationTokenService;
import com.valihameed.ufcfightpredictor.repository.roleRepository;
import com.valihameed.ufcfightpredictor.users.role;
import com.valihameed.ufcfightpredictor.users.user;
import com.valihameed.ufcfightpredictor.users.userService;
import com.valihameed.ufcfightpredictor.util.InputSanitizer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class RegistrationServiceTest {

    @Mock private userService userService;
    @Mock private emailValidator emailValidator;
    @Mock private roleRepository roleRepository;
    @Mock private ConfirmationTokenService confirmationTokenService;
    @Mock private EmailSender emailSender;
    @Mock private InputSanitizer inputSanitizer;

    private RegistrationService underTest;

    @BeforeEach
    void setUp() {
        underTest = new RegistrationService(
                userService,
                emailValidator,
                roleRepository,
                confirmationTokenService,
                emailSender,
                inputSanitizer
        );
    }

    @Test
    void canRegisterNewUser() {
        // Given
        RegistrationRequest request = new RegistrationRequest(
                "John", "Doe", "john@example.com", "password123", "johndoe"
        );
        role userRole = new role(1L, "ROLE_USER");

        given(emailValidator.test(anyString())).willReturn(true);
        given(inputSanitizer.sanitize(anyString())).willAnswer(i -> i.getArgument(0));
        given(roleRepository.findByName("ROLE_USER")).willReturn(Optional.of(userRole));
        given(userService.signUpUser(any(user.class))).willReturn("test-token-123");

        // When
        String token = underTest.register(request);

        // Then
        assertThat(token).isEqualTo("test-token-123");
        verify(emailSender).sendEmail(eq("john@example.com"), anyString(), eq("Confirm your email"));
    }

    @Test
    void registerWillThrowWhenEmailIsInvalid() {
        // Given
        RegistrationRequest request = new RegistrationRequest(
                "John", "Doe", "invalid-email", "password123", "johndoe"
        );
        given(emailValidator.test(anyString())).willReturn(false);

        // When / Then
        assertThatThrownBy(() -> underTest.register(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid email address");
    }

    @Test
    void canConfirmToken() {
        // Given
        String token = "test-token-123";
        user testUser = new user();
        testUser.setEmail("john@example.com");

        ConfirmationToken confirmationToken = new ConfirmationToken(
                token,
                LocalDateTime.now().minusMinutes(5),
                LocalDateTime.now().plusMinutes(15),
                testUser
        );
        
        given(confirmationTokenService.getToken(token)).willReturn(Optional.of(confirmationToken));

        // When
        String result = underTest.confirmToken(token);

        // Then
        assertThat(result).isEqualTo("confirmed");
        verify(confirmationTokenService).setConfirmedAt(token);
        verify(userService).enableUser("john@example.com");
    }

    @Test
    void confirmTokenWillThrowWhenTokenExpired() {
        // Given
        String token = "test-token-123";
        ConfirmationToken confirmationToken = new ConfirmationToken(
                token,
                LocalDateTime.now().minusMinutes(20),
                LocalDateTime.now().minusMinutes(5), // Expired 5 mins ago
                new user()
        );
        
        given(confirmationTokenService.getToken(token)).willReturn(Optional.of(confirmationToken));

        // When / Then
        assertThatThrownBy(() -> underTest.confirmToken(token))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("token expired");
    }

    @Test
    void confirmTokenWillThrowWhenAlreadyConfirmed() {
        // Given
        String token = "test-token-123";
        ConfirmationToken confirmationToken = new ConfirmationToken(
                token,
                LocalDateTime.now().minusMinutes(20),
                LocalDateTime.now().plusMinutes(15),
                new user()
        );
        confirmationToken.setConfirmedAt(LocalDateTime.now().minusMinutes(10));
        
        given(confirmationTokenService.getToken(token)).willReturn(Optional.of(confirmationToken));

        // When / Then
        assertThatThrownBy(() -> underTest.confirmToken(token))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("email already confirmed");
    }

    @Test
    void canResendVerificationToken() {
        // Given
        user testUser = new user();
        testUser.setEmail("john@example.com");
        testUser.setFirstName("John");
        testUser.setEnabled(false);

        given(userService.loadUserByEmail("john@example.com")).willReturn(testUser);
        given(userService.generateNewVerificationToken(testUser)).willReturn("new-token");

        // When
        String result = underTest.resendVerificationToken("john@example.com");

        // Then
        assertThat(result).isEqualTo("Verification email sent");
        verify(emailSender).sendEmail(eq("john@example.com"), anyString(), eq("Confirm your email"));
    }

    @Test
    void resendWillThrowWhenAlreadyVerified() {
        // Given
        user testUser = new user();
        testUser.setEmail("john@example.com");
        testUser.setEnabled(true);

        given(userService.loadUserByEmail("john@example.com")).willReturn(testUser);

        // When / Then
        assertThatThrownBy(() -> underTest.resendVerificationToken("john@example.com"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Account is already verified");
    }
}
