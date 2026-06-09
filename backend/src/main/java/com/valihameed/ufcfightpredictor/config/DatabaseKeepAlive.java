package com.valihameed.ufcfightpredictor.config;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class DatabaseKeepAlive {

    private final JdbcTemplate jdbcTemplate;

    public DatabaseKeepAlive(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    // Runs every 4 minutes (240,000 milliseconds)
    @Scheduled(fixedRate = 240000)
    public void pingDatabase() {
        try {
            jdbcTemplate.execute("SELECT 1");
            log.debug("Pinged Neon database to prevent sleep.");
        } catch (Exception e) {
            log.error("Failed to ping Neon database", e);
        }
    }
}
