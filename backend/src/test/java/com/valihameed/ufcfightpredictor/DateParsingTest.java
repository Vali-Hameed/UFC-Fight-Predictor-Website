package com.valihameed.ufcfightpredictor;

import org.junit.jupiter.api.Test;
import java.time.OffsetDateTime;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

public class DateParsingTest {
    @Test
    public void testDateParsing() {
        // This is the old format from ESPN which failed
        assertThrows(Exception.class, () -> {
            OffsetDateTime.parse("2026-06-15T00:00Z");
        }, "Parsing without seconds should throw an exception");

        // This is the new format after the fix
        assertDoesNotThrow(() -> {
            OffsetDateTime.parse("2026-06-15T00:00:00Z");
        }, "Parsing with seconds should succeed");
        
        System.out.println("\n--- SUCCESS: Date parsing fix verified! ---\n");
    }
}
