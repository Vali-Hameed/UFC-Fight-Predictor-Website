package com.valihameed.UFCFightPredictor.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

@RestController
@RequestMapping("/api/v1/fighters")
public class FighterController {

    private final RestTemplate restTemplate;

    @Value("${scraper.url:http://localhost:8001}")
    private String scraperBaseUrl;

    public FighterController() {
        this.restTemplate = new RestTemplate();
    }

    @GetMapping
    public ResponseEntity<String> getFighters() {
        try {
            String url = scraperBaseUrl + "/fighters";
            String response = restTemplate.getForObject(url, String.class);
            return ResponseEntity.ok().header("Content-Type", "application/json").body(response);
        } catch (Exception e) {
            return ResponseEntity.status(503).header("Content-Type", "application/json").body("{\"Active\": {}, \"Inactive\": {}}");
        }
    }
}
