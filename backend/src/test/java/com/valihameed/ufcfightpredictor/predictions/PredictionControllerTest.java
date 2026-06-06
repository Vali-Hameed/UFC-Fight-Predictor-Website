package com.valihameed.ufcfightpredictor.predictions;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.valihameed.ufcfightpredictor.models.UserPrediction;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PredictionController.class)
@AutoConfigureMockMvc(addFilters = false)
@org.springframework.test.context.ActiveProfiles("test")
public class PredictionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PredictionService predictionService;

    @MockBean
    private InputSanitizer inputSanitizer;

    @MockBean
    private com.valihameed.ufcfightpredictor.security.JwtService jwtService;

    @MockBean
    private org.springframework.security.core.userdetails.UserDetailsService userDetailsService;

    @Autowired
    private ObjectMapper objectMapper;

    private Authentication authentication;

    @BeforeEach
    void setUp() {
        user testUser = new user();
        testUser.setId(1L);
        testUser.setUsername("johndoe");

        authentication = new UsernamePasswordAuthenticationToken(testUser, null, Collections.emptyList());
    }

    @Test
    void canSubmitPrediction() throws Exception {
        // Given
        PredictionRequest request = new PredictionRequest(10L, "Conor", "KO/TKO", 2);

        given(inputSanitizer.sanitize(any(String.class))).willAnswer(i -> i.getArgument(0));
        given(predictionService.submitPrediction(anyLong(), any(PredictionRequest.class))).willReturn(new UserPrediction());

        // When / Then
        mockMvc.perform(post("/api/v1/predictions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .principal(authentication))
                .andExpect(status().isOk());

        verify(inputSanitizer).sanitize("Conor");
        verify(inputSanitizer).sanitize("KO/TKO");
    }

    @Test
    void submitPredictionValidatesInput() throws Exception {
        // Given
        PredictionRequest request = new PredictionRequest(null, "", "", -1);

        // When / Then
        mockMvc.perform(post("/api/v1/predictions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .principal(authentication))
                .andExpect(status().isBadRequest());
    }
}
