package com.gmscan.utility;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility class for extracting and validating text using regular expressions.
 * Provides methods to extract phone numbers, emails, websites, and detect names, addresses, and job titles.
 */
public class RegexUtils {

    /*
     * Regular expression patterns for different types of data extraction.
     * - PHONE_PATTERN: Matches phone numbers with optional country code and various formats.
     * - EMAIL_PATTERN: Matches standard email addresses.
     * - WEBSITE_PATTERN: Matches URLs with or without "www" or "http/https".
     * - ADDRESS_PATTERN: Detects common address formats including street names and ZIP codes.
     */
    public static final String PHONE_PATTERN = "(?:\\+?\\d{1,3}[-.]?)?\\s*(?:\\(?\\d{3}\\)?[-.]?)?\\s*\\d{3}[-.]?\\d{4}";

    public static final String EMAIL_PATTERN = "\\b[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}\\b";

    public static final String WEBSITE_PATTERN = "\\b(?:https?://)?(?:www\\.)?[A-Za-z0-9-]+(?:\\.[A-Za-z]{2,})+(?:/[A-Za-z0-9-._~:/?#\\[\\]@!$&'()*+,;=]*)?\\b";

    public static final String ADDRESS_PATTERN = "(?i)\\b\\d+\\s+(?:[A-Za-z0-9.-]+\\s*)+" +
            "(?:Street|St|Avenue|Ave|Road|Rd|Boulevard|Blvd|Lane|Ln|Drive|Dr|Circle|Cir|Court|Ct|Plaza|Square|Sq|Highway|Hwy|Suite|Unit|Floor|Fl)\\b" +
            "(?:,?\\s*(?:Apt|Suite|Unit|Room|Floor|Fl)\\.?\\s*[#]?\\d+)?\\s*" +
            "(?:,\\s*[A-Za-z]+(?:\\s+[A-Za-z]+)*,\\s*[A-Z]{2}\\s*,?\\s*\\d{5}(?:-\\d{4})?)?";

    public static final String NAME_PATTERN = "^(?!.*(?:LLC|Inc|Corp|Ltd))(?:[A-Z][a-zA-Z.'`-]+\\s?)+$";

    public static final String JOB_TITLE_PATTERN = "(?i)\\b(?:(?:Senior|Sr|Junior|Jr|Lead|Chief|Head|Principal|Associate)\\s+)?" +
            "(?:Software|UI/UX|UX/UI|Web|Mobile|Full[- ]Stack|Front[- ]End|Back[- ]End|DevOps|Cloud|Data|Product|Project|Program|Business|Marketing|Sales)?" +
            "\\s*(?:Developer|Designer|Engineer|Manager|Director|Architect|Consultant|Analyst|Administrator|Coordinator|Specialist|Officer|Executive)\\b";


    /**
     * Extracts phone numbers from a given text.
     * @param text The input text to search.
     * @return A list of phone numbers found.
     */
    public static ArrayList<String> extractPhoneNumbers(String text) {
        ArrayList<String> numbers = extractMatches(text, PHONE_PATTERN);
        // Clean up phone numbers
        for (int i = 0; i < numbers.size(); i++) {
            String number = numbers.get(i)
                    .replaceAll("[^+0-9]", "") // Remove all non-numeric chars except +
                    .replaceFirst("^00", "+"); // Convert 00 to + at start
            numbers.set(i, number);
        }
        return numbers;
    }

    /**
     * Extracts email addresses from a given text.
     * @param text The input text to search.
     * @return A list of email addresses found.
     */
    public static ArrayList<String> extractEmails(String text) {
        return extractMatches(text, EMAIL_PATTERN);
    }

    /**
     * Extracts website URLs from a given text.
     * @param text The input text to search.
     * @return A list of website URLs found.
     */
    public static ArrayList<String> extractWebsites(String text) {
        ArrayList<String> websites = extractMatches(text, WEBSITE_PATTERN);
        // Normalize websites
        for (int i = 0; i < websites.size(); i++) {
            String website = websites.get(i).toLowerCase();
            if (!website.startsWith("http")) {
                website = "http://" + website;
            }
            websites.set(i, website);
        }
        return websites;
    }

    /**
     * Checks if a given text matches an address pattern.
     * @param text The input text to check.
     * @return True if the text is likely an address, false otherwise.
     */
    public static boolean isAddress(String text) {
        return text.matches(ADDRESS_PATTERN);
    }

    /**
     * Checks if a given text is a name (starting with an uppercase letter, allowing spaces and hyphens).
     * @param text The input text to check.
     * @return True if the text is a name, false otherwise.
     */
    public static boolean isName(String text) {
        return text.matches(NAME_PATTERN);
    }

    /**
     * Checks if a given text is a job title.
     * @param text The input text to check.
     * @return True if the text is a job title, false otherwise.
     */
    public static boolean isJobTitle(String text) {
        return text.matches(JOB_TITLE_PATTERN);
    }

    /**
     * Helper method to extract matches from text using a given regex pattern.
     * @param text The input text to search.
     * @param pattern The regex pattern to match.
     * @return A list of extracted matches.
     */
    private static ArrayList<String> extractMatches(String text, String pattern) {
        ArrayList<String> matches = new ArrayList<>();
        Pattern compiledPattern = Pattern.compile(pattern);
        Matcher matcher = compiledPattern.matcher(text);
        while (matcher.find()) {
            matches.add(matcher.group().trim());
        }
        return matches;
    }

    public static boolean isValidEmail(String email) {
        return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches();
    }

    //At least 8 characters
    //At least one uppercase letter
    //At least one lowercase letter
    //At least one digit
    //At least one special character

    public static boolean isValidPassword(String password) {
        String passwordPattern = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=!])(?=\\S+$).{8,}$";
        return password.matches(passwordPattern);
    }

    public static boolean isPhoneNumber(String line) {
        return false;
    }
}