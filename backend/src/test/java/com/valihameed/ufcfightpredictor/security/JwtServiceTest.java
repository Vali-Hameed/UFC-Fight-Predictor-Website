package com.valihameed.ufcfightpredictor.security;

import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

public class JwtServiceTest {

    private JwtService underTest;

    @BeforeEach
    void setUp() {
        underTest = new JwtService();
        // Set the required @Value properties using ReflectionTestUtils
        ReflectionTestUtils.setField(underTest, "jwtSecret", "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970");
        ReflectionTestUtils.setField(underTest, "accessExpiryMinutes", 60L);
        underTest.init(); // Initialize the Key object
    }

    @Test
    void canGenerateAndParseToken() {
        // Given
        String subject = "john@example.com";
        Integer tokenVersion = 1;

        // When
        String token = underTest.generateToken(subject, tokenVersion);
        Claims claims = underTest.parseToken(token);

        // Then
        assertThat(token).isNotBlank();
        assertThat(claims.getSubject()).isEqualTo(subject);
        assertThat(claims.get("tokenVersion", Integer.class)).isEqualTo(1);
    }

    @Test
    void tokenIsValidReturnsTrueForValidToken() {
        // Given
        String token = underTest.generateToken("john@example.com", 1);

        // When
        boolean isValid = underTest.isTokenValid(token);

        // Then
        assertThat(isValid).isTrue();
    }

    @Test
    void tokenIsValidReturnsFalseForInvalidToken() {
        // When
        boolean isValid = underTest.isTokenValid("invalid.token.string");

        // Then
        assertThat(isValid).isFalse();
    }
}
