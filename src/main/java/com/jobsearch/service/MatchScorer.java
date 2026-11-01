package com.jobsearch.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Rule-based match score (0-100) between a job and the user's profile. No AI, no cost.
 *
 * <pre>
 *  Skills found in job text     up to 55 points
 *  Search keyword in job title  up to 20 points
 *  Location / remote fit        up to 15 points
 *  Language fit                 up to 10 points
 *  Excluded keyword in title    score capped at 10
 * </pre>
 */
public class MatchScorer {

    /** The profile data the scorer needs. */
    public record ProfileSnapshot(
            List<String> skills,
            List<String> searchKeywords,
            List<String> locations,
            String remotePreference,   // ANY | REMOTE | HYBRID | ONSITE
            boolean englishOnly,
            List<String> excludeKeywords) {
    }

    /** The job data the scorer needs. */
    public record JobInput(
            String title,
            String description,
            List<String> tags,
            String location,
            Boolean remote,
            String language,
            boolean germanRequired) {
    }

    public record Result(int score, List<String> matchedSkills, List<String> missingSkills) {
    }

    /** Skills are counted up to this many, so a long skill list doesn't punish every job. */
    private static final int SKILL_TARGET = 6;

    public Result score(JobInput job, ProfileSnapshot profile) {
        String title = lower(job.title());
        String fullText = title + "\n" + lower(job.description()) + "\n"
                + lower(String.join(" ", job.tags() == null ? List.of() : job.tags()));

        // 1. Skills
        List<String> matched = new ArrayList<>();
        List<String> missing = new ArrayList<>();
        for (String skill : profile.skills()) {
            if (containsTerm(fullText, skill)) {
                matched.add(skill);
            } else {
                missing.add(skill);
            }
        }
        int target = Math.max(1, Math.min(SKILL_TARGET, profile.skills().size()));
        double skillPoints = 55.0 * Math.min(1.0, matched.size() / (double) target);

        // 2. Title relevance
        double titlePoints = 0;
        for (String keyword : profile.searchKeywords()) {
            if (containsTerm(title, keyword)) {
                titlePoints = 20;
                break;
            }
            // partial credit: any significant word of the keyword in the title (e.g. "Java")
            for (String word : keyword.toLowerCase(Locale.ROOT).split("\\s+")) {
                if (word.length() > 2 && !isGenericWord(word) && containsTerm(title, word)) {
                    titlePoints = Math.max(titlePoints, 14);
                }
            }
        }

        // 3. Location / remote
        double locationPoints = locationPoints(job, profile);

        // 4. Language
        double languagePoints;
        if (!profile.englishOnly()) {
            languagePoints = 10;
        } else if (job.germanRequired()) {
            languagePoints = -15;
        } else {
            languagePoints = "en".equals(job.language()) ? 10 : 5;
        }

        int score = (int) Math.round(skillPoints + titlePoints + locationPoints + languagePoints);

        for (String excluded : profile.excludeKeywords()) {
            if (containsTerm(title, excluded)) {
                score = Math.min(score, 10);
                break;
            }
        }
        score = Math.max(0, Math.min(100, score));
        return new Result(score, matched, missing);
    }

    private double locationPoints(JobInput job, ProfileSnapshot profile) {
        boolean remote = Boolean.TRUE.equals(job.remote());
        String pref = profile.remotePreference() == null ? "ANY" : profile.remotePreference();
        if ("REMOTE".equals(pref)) {
            return remote ? 15 : 0;
        }
        if (remote) {
            return 15;
        }
        String location = lower(job.location());
        boolean nationwideOk = false;
        for (String wanted : profile.locations()) {
            String w = wanted.toLowerCase(Locale.ROOT).strip();
            if (w.equals("deutschland") || w.equals("germany")) {
                nationwideOk = true;
            } else if (!w.isEmpty() && location.contains(w)) {
                return 15;
            }
        }
        return nationwideOk ? 8 : 0;
    }

    /** Whole-word, case-insensitive match; "Spring Boot" also matches "spring-boot" / "springboot". */
    public static boolean containsTerm(String text, String term) {
        if (term == null || term.isBlank() || text == null || text.isEmpty()) {
            return false;
        }
        String[] parts = term.toLowerCase(Locale.ROOT).strip().split("\\s+");
        StringBuilder regex = new StringBuilder("(?<![\\p{L}\\p{N}])");
        for (int i = 0; i < parts.length; i++) {
            if (i > 0) {
                regex.append("[\\s\\-_]?");
            }
            regex.append(Pattern.quote(parts[i]));
        }
        regex.append("(?![\\p{L}\\p{N}])");
        // "Java" must not match "JavaScript": the look-ahead above handles that.
        return Pattern.compile(regex.toString()).matcher(text).find();
    }

    private static boolean isGenericWord(String w) {
        return switch (w) {
            case "developer", "engineer", "entwickler", "senior", "junior", "backend", "frontend", "software",
                 "full", "stack", "lead", "and", "und" -> true;
            default -> false;
        };
    }

    private static String lower(String s) {
        return s == null ? "" : s.toLowerCase(Locale.ROOT);
    }
}
