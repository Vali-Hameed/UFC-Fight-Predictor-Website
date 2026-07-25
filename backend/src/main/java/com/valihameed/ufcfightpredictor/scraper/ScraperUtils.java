package com.valihameed.ufcfightpredictor.scraper;

import com.valihameed.ufcfightpredictor.models.Fight;
import java.util.List;

public class ScraperUtils {

    public static Fight fuzzyMatchFight(List<Fight> dbFights, String f1Name, String f2Name) {
        // Strict match (both fighters) per user request, but allow fuzzy name matching
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
        if (dbName.equalsIgnoreCase(scraperName)) return true;
        
        // Match by last name
        String[] dbParts = dbName.split(" ");
        String[] scraperParts = scraperName.split(" ");
        
        if (dbParts.length > 0 && scraperParts.length > 0) {
            String dbLast = dbParts[dbParts.length - 1].toLowerCase();
            String scraperLast = scraperParts[scraperParts.length - 1].toLowerCase();
            
            boolean lastNameMatch = dbLast.equals(scraperLast);
            
            // Substring/prefix matching for last names (e.g. "Said" vs "Saidov")
            if (!lastNameMatch && dbLast.length() >= 3 && scraperLast.length() >= 3) {
                lastNameMatch = dbLast.startsWith(scraperLast) || scraperLast.startsWith(dbLast);
            }
            
            if (lastNameMatch) {
                // Also verify first-name initial matches to avoid false positives
                // (e.g. "Anderson Silva" should not match "Erick Silva")
                if (dbParts.length >= 2 && scraperParts.length >= 2) {
                    char dbInitial = Character.toLowerCase(dbParts[0].charAt(0));
                    char scraperInitial = Character.toLowerCase(scraperParts[0].charAt(0));
                    return dbInitial == scraperInitial;
                }
                // If either name is a single word (mononym), accept the last name match
                return true;
            }
        }
        return false;
    }
}
