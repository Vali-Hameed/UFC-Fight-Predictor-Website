package com.valihameed.ufcfightpredictor.github;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j
public class GitHubActionsTriggerService {

    private final RestTemplate restTemplate;

    @Value("${github.actions.enabled:true}")
    private boolean enabled;

    @Value("${github.actions.token:}")
    private String githubToken;

    @Value("${github.actions.owner:Vali-Hameed}")
    private String repoOwner;

    @Value("${github.actions.repo:UFC-Fight-Predictor}")
    private String repoName;

    @Value("${github.actions.workflow-id:retrain-and-deploy.yml}")
    private String workflowId;

    public GitHubActionsTriggerService() {
        this.restTemplate = new RestTemplate();
    }

    public GitHubActionsTriggerService(RestTemplate restTemplate,
                                       boolean enabled,
                                       String githubToken,
                                       String repoOwner,
                                       String repoName,
                                       String workflowId) {
        this.restTemplate = restTemplate;
        this.enabled = enabled;
        this.githubToken = githubToken;
        this.repoOwner = repoOwner;
        this.repoName = repoName;
        this.workflowId = workflowId;
    }

    @Async
    public void triggerMlRetraining(Long eventId, String eventName) {
        if (!enabled) {
            log.debug("GitHub Actions workflow dispatch is disabled via config.");
            return;
        }

        if (githubToken == null || githubToken.trim().isEmpty()) {
            log.info("GitHub Actions token is not configured. Skipping automated ML retraining dispatch for event: {} (ID: {})",
                    eventName, eventId);
            return;
        }

        String url = String.format("https://api.github.com/repos/%s/%s/actions/workflows/%s/dispatches",
                repoOwner, repoName, workflowId);

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(githubToken.trim());
            headers.set("Accept", "application/vnd.github+json");
            headers.set("X-GitHub-Api-Version", "2022-11-28");

            Map<String, Object> body = new HashMap<>();
            body.put("ref", "main");

            Map<String, String> inputs = new HashMap<>();
            inputs.put("trigger_source", "backend_completion");
            inputs.put("event_name", eventName != null ? eventName : "Event " + eventId);
            body.put("inputs", inputs);

            HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(body, headers);

            log.info("Triggering GitHub Actions workflow {} on {}/{} for event '{}' (ID: {})...",
                    workflowId, repoOwner, repoName, eventName, eventId);

            ResponseEntity<Void> response = restTemplate.exchange(url, HttpMethod.POST, requestEntity, Void.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("Successfully dispatched GitHub Actions ML retraining workflow for event '{}' (HTTP {})",
                        eventName, response.getStatusCode());
            } else {
                log.warn("GitHub Actions workflow dispatch returned status: {}", response.getStatusCode());
            }
        } catch (Exception e) {
            log.error("Failed to trigger GitHub Actions ML retraining workflow for event '{}': {}",
                    eventName, e.getMessage());
        }
    }
}
