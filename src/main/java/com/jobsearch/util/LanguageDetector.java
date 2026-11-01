package com.jobsearch.util;

import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Very light heuristics: is a posting written in German or English, and does it
 * require German language skills? Good enough for filtering; no external library needed.
 */
public final class LanguageDetector {

    private static final Set<String> GERMAN_WORDS = Set.of(
            "und", "der", "die", "das", "wir", "sie", "für", "mit", "ihre", "ihr", "bei", "eine",
            "einen", "sind", "von", "zu", "auf", "unser", "unsere", "deine", "du", "dich", "oder", "nicht");
    private static final Set<String> ENGLISH_WORDS = Set.of(
            "and", "the", "we", "you", "with", "for", "our", "your", "are", "of", "to", "in",
            "is", "on", "will", "be", "as", "an", "or", "this");

    private static final Pattern GERMAN_REQUIRED = Pattern.compile(
            "(?iu)(sehr gute|gute|fließende|fliessende|verhandlungssichere|exzellente)\\s+deutschkenntnisse"
                    + "|deutschkenntnisse\\s*\\(?(c1|c2|b2)"
                    + "|deutsch\\s*(c1|c2|b2|fließend|fliessend|verhandlungssicher)"
                    + "|(fluent|business|native)[-\\s]+(level\\s+)?german"
                    + "|german\\s*\\(?(c1|c2|b2|fluent|native)"
                    + "|german (is )?(required|mandatory|a must)"
                    + "|(fluency|proficiency) in german"
                    + "|deutsch und englisch"
                    + "|german and english");

    private LanguageDetector() {
    }

    /** Returns "de", "en" or "unknown". */
    public static String detect(String text) {
        if (text == null || text.isBlank()) {
            return "unknown";
        }
        int de = 0;
        int en = 0;
        for (String word : text.toLowerCase(Locale.ROOT).split("[^\\p{L}]+")) {
            if (GERMAN_WORDS.contains(word)) {
                de++;
            } else if (ENGLISH_WORDS.contains(word)) {
                en++;
            }
        }
        if (de + en < 5) {
            return "unknown";
        }
        return de > en ? "de" : "en";
    }

    /** True when the job is written in German or explicitly asks for strong German skills. */
    public static boolean germanRequired(String text, String language) {
        if ("de".equals(language)) {
            return true;
        }
        return text != null && GERMAN_REQUIRED.matcher(text).find();
    }
}
