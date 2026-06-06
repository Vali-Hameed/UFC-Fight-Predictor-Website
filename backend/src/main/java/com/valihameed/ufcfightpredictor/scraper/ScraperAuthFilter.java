package com.valihameed.ufcfightpredictor.scraper;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.http.HttpStatus;

import java.io.IOException;

@Component
public class ScraperAuthFilter extends OncePerRequestFilter {

    @Value("${scraper.api-key}")
    private String scraperApiKey;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String uri = request.getRequestURI();
        if (uri.startsWith("/api/v1/internal/scraper")) {
            // Allow frontend to GET/DELETE logs using JWT instead of Scraper Key
            if (uri.startsWith("/api/v1/internal/scraper/logs") && 
                (request.getMethod().equals("GET") || request.getMethod().equals("DELETE") || request.getMethod().equals("OPTIONS"))) {
                filterChain.doFilter(request, response);
                return;
            }
            
            String key = request.getHeader("X-Scraper-Key");
            if (key == null || !key.equals(scraperApiKey)) {
                response.setStatus(HttpStatus.FORBIDDEN.value());
                return;
            }
        }
        filterChain.doFilter(request, response);
    }
}
