package com.valihameed.ufcfightpredictor.scraper;

import com.valihameed.ufcfightpredictor.models.Fight;
import java.text.Normalizer;
import java.util.*;
import java.util.regex.Pattern;

public class ScraperUtils {

    private static final Pattern DIACRITICS = Pattern.compile("\\p{M}");
    private static final Pattern PUNCTUATION = Pattern.compile("[^a-zA-Z0-9\\s]");

    private static final Set<String> COMMON_SUFFIXES = Set.of(
            "jr", "jr.", "sr", "sr.", "ii", "iii", "iv",
            "uulu", "kyzy", "oglu", "ogli"
    );

    public static Fight fuzzyMatchFight(List<Fight> dbFights, String f1Name, String f2Name) {
        if (dbFights == null || f1Name == null || f2Name == null) return null;
        for (Fight fight : dbFights) {
            String dbF1 = fight.getFighter1Name();
            String dbF2 = fight.getFighter2Name();
            if ((isMatch(dbF1, f1Name) && isMatch(dbF2, f2Name)) || (isMatch(dbF1, f2Name) && isMatch(dbF2, f1Name))) {
                return fight;
            }
        }
        return null;
    }

    public static boolean isMatch(String dbName, String scraperName) {
        if (dbName == null || scraperName == null) return false;
        if (dbName.trim().equalsIgnoreCase(scraperName.trim())) return true;

        List<String> t1 = extractTokens(dbName);
        List<String> t2 = extractTokens(scraperName);
        if (t1.isEmpty() || t2.isEmpty()) return false;

        // 1. Exact match ignoring spaces/punctuation (e.g. "Sumudaerji" vs "Su Mudaerji", "DaUn Jung" vs "Da Un Jung")
        String joined1 = String.join("", t1);
        String joined2 = String.join("", t2);
        if (joined1.equalsIgnoreCase(joined2)) {
            return true;
        }

        // 2. Token set equality (handles Eastern surname-first conventions like "Zhang Weili" vs "Weili Zhang")
        if (new HashSet<>(t1).equals(new HashSet<>(t2))) {
            return true;
        }

        // 3. Match by last name
        String dbLast = t1.get(t1.size() - 1);
        String scraperLast = t2.get(t2.size() - 1);

        boolean lastNameMatch = dbLast.equals(scraperLast);

        // Substring / prefix matching for last names (e.g. "Said" vs "Saidov")
        if (!lastNameMatch && dbLast.length() >= 3 && scraperLast.length() >= 3) {
            lastNameMatch = dbLast.startsWith(scraperLast) || scraperLast.startsWith(dbLast);
        }

        // Check if last name is present anywhere in the other fighter's name tokens (compound surnames)
        if (!lastNameMatch) {
            lastNameMatch = t2.contains(dbLast) || t1.contains(scraperLast);
        }

        if (lastNameMatch) {
            // Also verify first-name initial matches to avoid false positives (e.g. "Anderson Silva" vs "Erick Silva")
            if (t1.size() >= 2 && t2.size() >= 2) {
                char dbInitial = t1.get(0).charAt(0);
                char scraperInitial = t2.get(0).charAt(0);
                return dbInitial == scraperInitial;
            }
            // If either name is a single word (mononym), accept the last name match
            return true;
        }

        return false;
    }

    private static List<String> extractTokens(String name) {
        if (name == null || name.isBlank()) return Collections.emptyList();

        // Strip accents (e.g. José -> Jose)
        String normalized = Normalizer.normalize(name, Normalizer.Form.NFD);
        String noAccents = DIACRITICS.matcher(normalized).replaceAll("");

        // Replace punctuation with spaces
        String clean = PUNCTUATION.matcher(noAccents).replaceAll(" ").toLowerCase().trim();
        if (clean.isBlank()) return Collections.emptyList();

        String[] parts = clean.split("\\s+");
        List<String> tokens = new ArrayList<>(Arrays.asList(parts));

        // Strip trailing suffixes if more than 1 token
        if (tokens.size() > 1 && COMMON_SUFFIXES.contains(tokens.get(tokens.size() - 1))) {
            tokens.remove(tokens.size() - 1);
        }

        return tokens;
    }
}
