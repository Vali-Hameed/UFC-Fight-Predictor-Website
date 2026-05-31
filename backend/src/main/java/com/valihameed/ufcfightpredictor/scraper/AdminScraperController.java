package com.valihameed.ufcfightpredictor.scraper;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

@RestController
@RequestMapping("/api/v1/admin/scraper")
public class AdminScraperController {

    @Value("${scraper.url:http://localhost:8001}")
    private String scraperUrl;

    private final RestTemplate restTemplate;

    public AdminScraperController() {
        this.restTemplate = new RestTemplate();
    }

    @PostMapping("/trigger")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> triggerScraper() {
        try {
            // Call the python scraper's /trigger endpoint
            String url = scraperUrl + "/trigger";
            ResponseEntity<String> response = restTemplate.postForEntity(url, null, String.class);
            return ResponseEntity.ok().body(response.getBody());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Failed to trigger scraper: " + e.getMessage());
        }
    }
}
