package com.valihameed.ufcfightpredictor.scraper;

import com.valihameed.ufcfightpredictor.models.Fight;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ScraperUtilsTest {

    @Test
    void isMatch_exactMatches() {
        assertTrue(ScraperUtils.isMatch("Jon Jones", "Jon Jones"));
        assertTrue(ScraperUtils.isMatch("jon jones", "JON JONES"));
        assertFalse(ScraperUtils.isMatch("Jon Jones", "Daniel Cormier"));
    }

    @Test
    void isMatch_lastNameMatchesSameInitial() {
        assertTrue(ScraperUtils.isMatch("Jon Jones", "Jonathan Jones"));
        assertTrue(ScraperUtils.isMatch("Khamzat Chimaev", "K. Chimaev"));
    }

    @Test
    void isMatch_lastNameMatchesDifferentInitialShouldFail() {
        // Two different fighters with the same last name should NOT match
        assertFalse(ScraperUtils.isMatch("Anderson Silva", "Erick Silva"));
        assertFalse(ScraperUtils.isMatch("Jon Jones", "Cedric Jones"));
    }

    @Test
    void isMatch_fuzzySubstringMatches() {
        // The reported issue: "Saidov" vs "Said" - mononyms (single word) should match
        assertTrue(ScraperUtils.isMatch("Saidov", "Said"));
        assertTrue(ScraperUtils.isMatch("Said", "Saidov"));
        
        // With first names - initials must match
        assertTrue(ScraperUtils.isMatch("Said Saidov", "Said Said"));
        
        // Ensure it doesn't match completely unrelated names
        assertFalse(ScraperUtils.isMatch("Saidov", "Smith"));
        
        // Other examples of fuzzy matches (mononyms)
        assertTrue(ScraperUtils.isMatch("Nurmagomedov", "Nurmagomed"));
    }

    @Test
    void isMatch_nullAndEdgeCases() {
        assertFalse(ScraperUtils.isMatch(null, "Jon Jones"));
        assertFalse(ScraperUtils.isMatch("Jon Jones", null));
        assertFalse(ScraperUtils.isMatch(null, null));
    }

    @Test
    void fuzzyMatchFight_exactMatch() {
        Fight fight = new Fight();
        fight.setFighter1Name("Jon Jones");
        fight.setFighter2Name("Ciryl Gane");

        List<Fight> dbFights = Arrays.asList(fight);

        // Same order
        Fight matched = ScraperUtils.fuzzyMatchFight(dbFights, "Jon Jones", "Ciryl Gane");
        assertNotNull(matched);
        assertEquals(fight, matched);

        // Reversed order
        Fight matchedReversed = ScraperUtils.fuzzyMatchFight(dbFights, "Ciryl Gane", "Jon Jones");
        assertNotNull(matchedReversed);
        assertEquals(fight, matchedReversed);
    }

    @Test
    void fuzzyMatchFight_fuzzyMatch() {
        Fight fight = new Fight();
        fight.setFighter1Name("Muslim Salikhov");
        fight.setFighter2Name("Abubakar Nurmagomedov");

        Fight fight2 = new Fight();
        fight2.setFighter1Name("Said Saidov");
        fight2.setFighter2Name("John Doe");

        List<Fight> dbFights = Arrays.asList(fight, fight2);

        // Scraper brings "Said" instead of "Saidov" - same first initial 'S'
        Fight matched = ScraperUtils.fuzzyMatchFight(dbFights, "Said Said", "John Doe");
        assertNotNull(matched);
        assertEquals("Said Saidov", matched.getFighter1Name());
    }

    @Test
    void fuzzyMatchFight_noMatch() {
        Fight fight = new Fight();
        fight.setFighter1Name("Jon Jones");
        fight.setFighter2Name("Ciryl Gane");

        List<Fight> dbFights = Arrays.asList(fight);

        Fight matched = ScraperUtils.fuzzyMatchFight(dbFights, "Jon Jones", "Stipe Miocic");
        assertNull(matched);
    }

    @Test
    void fuzzyMatchFight_doesNotFalseMatchSameLastNameDifferentFighter() {
        Fight fight1 = new Fight();
        fight1.setFighter1Name("Anderson Silva");
        fight1.setFighter2Name("Israel Adesanya");

        Fight fight2 = new Fight();
        fight2.setFighter1Name("Erick Silva");
        fight2.setFighter2Name("Mike Perry");

        List<Fight> dbFights = Arrays.asList(fight1, fight2);

        // Should match fight1 (Anderson), NOT fight2 (Erick)
        Fight matched = ScraperUtils.fuzzyMatchFight(dbFights, "A. Silva", "Israel Adesanya");
        assertNotNull(matched);
        assertEquals("Anderson Silva", matched.getFighter1Name());
    }
}
