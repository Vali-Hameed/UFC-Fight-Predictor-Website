package com.valihameed.ufcfightpredictor.security;

import com.valihameed.ufcfightpredictor.users.user;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetailsService;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class JwtAuthenticationFilterTest {

    @Mock
    private JwtService jwtService;

    @Mock
    private UserDetailsService userDetailsService;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    @InjectMocks
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void doFilterInternal_whenValidToken_thenSetsAuthentication() throws Exception {
        // Given
        given(request.getHeader("Authorization")).willReturn("Bearer valid-token");
        given(jwtService.isTokenValid("valid-token")).willReturn(true);
        
        io.jsonwebtoken.Claims claims = mock(io.jsonwebtoken.Claims.class);
        given(jwtService.parseToken("valid-token")).willReturn(claims);
        given(claims.getSubject()).willReturn("johndoe");
        given(claims.get("tokenVersion", Integer.class)).willReturn(1);

        user testUser = new user();
        testUser.setUsername("johndoe");
        testUser.setTokenVersion(1);
        com.valihameed.ufcfightpredictor.users.role userRole = new com.valihameed.ufcfightpredictor.users.role();
        userRole.setName("ROLE_USER");
        testUser.setRole(userRole);

        given(userDetailsService.loadUserByUsername("johndoe")).willReturn(testUser);

        // When
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Then
        assertNotNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doFilterInternal_whenNoToken_thenContinuesWithoutAuthentication() throws Exception {
        // Given
        given(request.getHeader("Authorization")).willReturn(null);

        // When
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Then
        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain).doFilter(request, response);
    }
}
