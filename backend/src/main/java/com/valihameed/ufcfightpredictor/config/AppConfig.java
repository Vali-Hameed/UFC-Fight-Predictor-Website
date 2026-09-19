package com.valihameed.ufcfightpredictor.config;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.List;

@Configuration
public class AppConfig {
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

    /**
     * Dedicated RestTemplate for ESPN API calls.
     * Includes browser-like headers to avoid Akamai CDN bot detection,
     * plus connection/read timeouts.
     */
    @Bean("espnRestTemplate")
    public RestTemplate espnRestTemplate(RestTemplateBuilder builder) {
        ClientHttpRequestInterceptor headerInterceptor = (request, body, execution) -> {
            HttpHeaders headers = request.getHeaders();
            headers.set("User-Agent", "curl/8.4.0");
            headers.set("Accept", "application/json");
            headers.set("Accept-Language", "en-US,en;q=0.9");
            return execution.execute(request, body);
        };

        return builder
                .connectTimeout(Duration.ofSeconds(5))
                .readTimeout(Duration.ofSeconds(10))
                .interceptors(List.of(headerInterceptor))
                .build();
    }
}
