package com.valihameed.ufcfightpredictor.forum;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.valihameed.ufcfightpredictor.models.ForumThread;
import com.valihameed.ufcfightpredictor.models.ThreadSubscription;
import com.valihameed.ufcfightpredictor.repository.ForumThreadRepository;
import com.valihameed.ufcfightpredictor.repository.ThreadSubscriptionRepository;
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
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ForumThreadController.class)
@AutoConfigureMockMvc(addFilters = false)
@org.springframework.test.context.ActiveProfiles("test")
public class ForumThreadControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean private ForumThreadRepository forumThreadRepository;
    @MockBean private ThreadSubscriptionRepository threadSubscriptionRepository;
    @MockBean private InputSanitizer inputSanitizer;
    @MockBean private com.valihameed.ufcfightpredictor.security.JwtService jwtService;
    @MockBean private org.springframework.security.core.userdetails.UserDetailsService userDetailsService;

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
    void canListThreadsByEventId() throws Exception {
        // Given
        ForumThread thread1 = new ForumThread();
        thread1.setId(10L);
        thread1.setEventId(100L);
        thread1.setTitle("UFC 300 Discussion");

        given(forumThreadRepository.findByEventId(100L)).willReturn(List.of(thread1));

        // When / Then
        mockMvc.perform(get("/api/v1/forum/threads?eventId=100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size()").value(1))
                .andExpect(jsonPath("$[0].title").value("UFC 300 Discussion"));
    }

    @Test
    void canCreateThread() throws Exception {
        // Given
        ForumThreadController.ForumThreadRequest request = new ForumThreadController.ForumThreadRequest();
        request.setEventId(100L);
        request.setTitle("My Title");

        ForumThread savedThread = new ForumThread();
        savedThread.setId(10L);
        savedThread.setTitle("My Title Sanitzed");

        given(inputSanitizer.sanitize("My Title")).willReturn("My Title Sanitzed");
        given(forumThreadRepository.save(any(ForumThread.class))).willReturn(savedThread);

        // When / Then
        mockMvc.perform(post("/api/v1/forum/threads")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .principal(authentication))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("My Title Sanitzed"));
    }

    @Test
    void canToggleSubscription() throws Exception {
        // Given
        given(threadSubscriptionRepository.findByUserIdAndThreadId(1L, 10L)).willReturn(Optional.empty());

        // When / Then
        mockMvc.perform(post("/api/v1/forum/threads/10/subscribe")
                        .principal(authentication))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(true));

        verify(threadSubscriptionRepository).save(any(ThreadSubscription.class));
    }
}
