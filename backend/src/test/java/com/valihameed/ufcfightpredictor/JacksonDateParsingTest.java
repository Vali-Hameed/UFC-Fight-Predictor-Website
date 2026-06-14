package com.valihameed.ufcfightpredictor;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.JsonTest;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.OffsetDateTime;
import com.valihameed.ufcfightpredictor.models.Event;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

@JsonTest
public class JacksonDateParsingTest {

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    public void testJacksonParsing() throws Exception {
        String jsonWithoutSeconds = "{\"name\":\"UFC Freedom 250\", \"eventDate\":\"2026-06-15T00:00Z\"}";
        String jsonWithSeconds = "{\"name\":\"UFC Freedom 250\", \"eventDate\":\"2026-06-15T00:00:00Z\"}";

        System.out.println("\n--- RUNNING JACKSON TESTS ---");
        
        try {
            objectMapper.readValue(jsonWithoutSeconds, Event.class);
            System.out.println("JSON Without Seconds: SUCCESS (Unexpected!)");
        } catch (Exception e) {
            System.out.println("JSON Without Seconds: FAILED as expected - " + e.getMessage());
        }

        try {
            objectMapper.readValue(jsonWithSeconds, Event.class);
            System.out.println("JSON With Seconds: SUCCESS (Expected!)");
        } catch (Exception e) {
            System.out.println("JSON With Seconds: FAILED (Unexpected!) - " + e.getMessage());
        }
        
        System.out.println("-----------------------------\n");
    }
}
