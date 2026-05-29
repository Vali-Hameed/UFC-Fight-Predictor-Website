package com.valihameed.ufcfightpredictor.scraper;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class ScraperAuthFilter extends OncePerRequestFilter {

    @Value("${scraper.api-key}")
    private String scraperApiKey;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String path = request.getRequestURI();
        if (path.startsWith("/api/v1/internal/scraper")) {
            String header = request.getHeader("X-Scraper-Key");
            if (header == null || !header.equals(scraperApiKey)) {
                response.setStatus(401);
                response.getWriter().write("Unauthorized scraper client");
                return;
            }
        }
        filterChain.doFilter(request, response);
    }
}
