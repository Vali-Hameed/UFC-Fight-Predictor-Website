package com.valihameed.ufcfightpredictor.github;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GitHubActionsTriggerServiceTest {

    @Mock
    private RestTemplate restTemplate;

    private GitHubActionsTriggerService service;

    @BeforeEach
    void setUp() {
        service = new GitHubActionsTriggerService(
                restTemplate,
                true,
                "ghp_test_token_12345",
                "Vali-Hameed",
                "UFC-Fight-Predictor",
                "retrain-and-deploy.yml"
        );
    }

    @Test
    void triggerMlRetraining_Success() {
        when(restTemplate.exchange(any(String.class), eq(HttpMethod.POST), any(HttpEntity.class), eq(Void.class)))
                .thenReturn(new ResponseEntity<>(HttpStatus.NO_CONTENT));

        service.triggerMlRetraining(100L, "UFC 305");

        ArgumentCaptor<String> urlCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<HttpEntity<Map<String, Object>>> entityCaptor = ArgumentCaptor.forClass(HttpEntity.class);

        verify(restTemplate, times(1)).exchange(
                urlCaptor.capture(),
                eq(HttpMethod.POST),
                entityCaptor.capture(),
                eq(Void.class)
        );

        assertThat(urlCaptor.getValue()).isEqualTo("https://api.github.com/repos/Vali-Hameed/UFC-Fight-Predictor/actions/workflows/retrain-and-deploy.yml/dispatches");

        HttpEntity<Map<String, Object>> capturedEntity = entityCaptor.getValue();
        assertThat(capturedEntity.getHeaders().getFirst("Authorization")).isEqualTo("Bearer ghp_test_token_12345");
        assertThat(capturedEntity.getHeaders().getFirst("Accept")).isEqualTo("application/vnd.github+json");

        Map<String, Object> body = capturedEntity.getBody();
        assertThat(body).isNotNull();
        assertThat(body.get("ref")).isEqualTo("main");

        @SuppressWarnings("unchecked")
        Map<String, String> inputs = (Map<String, String>) body.get("inputs");
        assertThat(inputs).isNotNull();
        assertThat(inputs.get("trigger_source")).isEqualTo("backend_completion");
        assertThat(inputs.get("event_name")).isEqualTo("UFC 305");
    }

    @Test
    void triggerMlRetraining_SkipsWhenTokenMissing() {
        GitHubActionsTriggerService serviceNoToken = new GitHubActionsTriggerService(
                restTemplate,
                true,
                "",
                "Vali-Hameed",
                "UFC-Fight-Predictor",
                "retrain-and-deploy.yml"
        );

        serviceNoToken.triggerMlRetraining(100L, "UFC 305");

        verifyNoInteractions(restTemplate);
    }

    @Test
    void triggerMlRetraining_SkipsWhenDisabled() {
        GitHubActionsTriggerService disabledService = new GitHubActionsTriggerService(
                restTemplate,
                false,
                "ghp_test_token_12345",
                "Vali-Hameed",
                "UFC-Fight-Predictor",
                "retrain-and-deploy.yml"
        );

        disabledService.triggerMlRetraining(100L, "UFC 305");

        verifyNoInteractions(restTemplate);
    }

    @Test
    void triggerMlRetraining_HandlesHttpExceptionGracefully() {
        when(restTemplate.exchange(any(String.class), eq(HttpMethod.POST), any(HttpEntity.class), eq(Void.class)))
                .thenThrow(new RestClientException("Connection timeout"));

        assertThatCode(() -> service.triggerMlRetraining(100L, "UFC 305"))
                .doesNotThrowAnyException();

        verify(restTemplate, times(1)).exchange(any(String.class), eq(HttpMethod.POST), any(HttpEntity.class), eq(Void.class));
    }
}
