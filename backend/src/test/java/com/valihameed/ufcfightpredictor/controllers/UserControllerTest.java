package com.valihameed.ufcfightpredictor.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.valihameed.ufcfightpredictor.repository.*;
import com.valihameed.ufcfightpredictor.users.user;
import com.valihameed.ufcfightpredictor.util.InputSanitizer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(userController.class)
@AutoConfigureMockMvc(addFilters = false)
@org.springframework.test.context.ActiveProfiles("test")
public class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean private userRepository userRepository;
    @MockBean private InputSanitizer inputSanitizer;
    @MockBean private LeaderboardRepository leaderboardRepository;
    @MockBean private UserPredictionRepository userPredictionRepository;
    @MockBean private FightRepository fightRepository;
    @MockBean private EventRepository eventRepository;
    @MockBean private PredictionResultRepository predictionResultRepository;
    @MockBean private com.valihameed.ufcfightpredictor.security.JwtService jwtService;
    @MockBean private org.springframework.security.core.userdetails.UserDetailsService userDetailsService;

    @Autowired
    private ObjectMapper objectMapper;

    private user testUser;
    private Authentication authentication;

    @BeforeEach
    void setUp() {
        testUser = new user();
        testUser.setId(1L);
        testUser.setUsername("johndoe");
        testUser.setFirstName("John");
        testUser.setLastName("Doe");
        testUser.setPublicProfile(true);

        authentication = new UsernamePasswordAuthenticationToken(testUser, null, Collections.emptyList());
    }

    @Test
    void canGetMe() throws Exception {
        // Given
        given(leaderboardRepository.findByUserId(1L)).willReturn(Optional.empty());
        given(userPredictionRepository.findByUserId(1L)).willReturn(Collections.emptyList());

        // When / Then
        mockMvc.perform(get("/api/v1/users/me").principal(authentication))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("johndoe"))
                .andExpect(jsonPath("$.firstName").value("John"));
    }

    @Test
    void canGetUserByUsername() throws Exception {
        // Given
        given(userRepository.findByUsername("johndoe")).willReturn(Optional.of(testUser));
        given(leaderboardRepository.findByUserId(1L)).willReturn(Optional.empty());
        given(userPredictionRepository.findByUserId(1L)).willReturn(Collections.emptyList());

        // When / Then
        mockMvc.perform(get("/api/v1/users/johndoe").principal(authentication))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("johndoe"));
    }

    @Test
    void canUpdateMe() throws Exception {
        // Given
        userController.UpdateProfileRequest request = new userController.UpdateProfileRequest();
        request.setFirstName("Johnny");
        
        given(inputSanitizer.sanitize("Johnny")).willReturn("Johnny");
        given(leaderboardRepository.findByUserId(1L)).willReturn(Optional.empty());
        given(userPredictionRepository.findByUserId(1L)).willReturn(Collections.emptyList());

        // When / Then
        mockMvc.perform(put("/api/v1/users/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .principal(authentication))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("Johnny"));
    }
}
