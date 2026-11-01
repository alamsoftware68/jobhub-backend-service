package com.jobsearch;

import com.jobsearch.service.MatchScorer;
import com.jobsearch.util.LanguageDetector;
import com.jobsearch.util.TextUtils;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MatchScorerTest {

    private final MatchScorer scorer = new MatchScorer();

    private MatchScorer.ProfileSnapshot profile(boolean englishOnly) {
        return new MatchScorer.ProfileSnapshot(
                List.of("Java", "Spring Boot", "Microservices", "AWS", "React", "Jenkins"),
                List.of("Java Developer"),
                List.of("Berlin", "Deutschland"),
                "ANY",
                englishOnly,
                List.of("Werkstudent"));
    }

    @Test
    void strongMatchScoresHigh() {
        MatchScorer.Result r = scorer.score(new MatchScorer.JobInput(
                "Senior Java Developer",
                "Build microservices with Spring Boot on AWS. CI with Jenkins, frontend in React.",
                List.of(), "Berlin", false, "en", false), profile(false));
        assertTrue(r.score() >= 90, "score was " + r.score());
        assertEquals(6, r.matchedSkills().size());
    }

    @Test
    void javaDoesNotMatchJavaScript() {
        assertFalse(MatchScorer.containsTerm("senior javascript developer", "Java"));
        assertTrue(MatchScorer.containsTerm("we use spring-boot 3", "Spring Boot"));
        assertTrue(MatchScorer.containsTerm("ci/cd pipelines", "CI/CD"));
    }

    @Test
    void germanRequiredIsPenalisedForEnglishOnly() {
        MatchScorer.JobInput job = new MatchScorer.JobInput("Java Developer", "Java Spring Boot AWS",
                List.of(), "Berlin", false, "de", true);
        int normal = scorer.score(job, profile(false)).score();
        int englishOnly = scorer.score(job, profile(true)).score();
        assertTrue(englishOnly < normal);
    }

    @Test
    void excludedKeywordCapsScore() {
        MatchScorer.Result r = scorer.score(new MatchScorer.JobInput("Werkstudent Java Developer",
                "Java Spring Boot AWS Microservices", List.of(), "Berlin", false, "en", false), profile(false));
        assertTrue(r.score() <= 10);
    }

    @Test
    void detectsLanguage() {
        assertEquals("de", LanguageDetector.detect("Wir suchen für unser Team eine Person, die mit uns und dem Kunden arbeitet."));
        assertEquals("en", LanguageDetector.detect("We are looking for an engineer to join our team and you will work with the product."));
        assertTrue(LanguageDetector.germanRequired("Requirements: fluent German and English", "en"));
        assertFalse(LanguageDetector.germanRequired("Requirements: fluent English", "en"));
    }

    @Test
    void resolvesCountries() {
        assertEquals("Germany", com.jobsearch.util.Countries.normalize("DEUTSCHLAND"));
        assertEquals("United Kingdom", com.jobsearch.util.Countries.fromLocation("London, Greater London, United Kingdom"));
        assertEquals("Netherlands", com.jobsearch.util.Countries.fromLocation("Breda"));
        assertEquals("Germany", com.jobsearch.util.Countries.fromLocation("München"));
        assertEquals("Austria", com.jobsearch.util.Countries.normalize("Österreich"));
        assertEquals("gb", com.jobsearch.util.Countries.adzunaCode("UK"));
        assertTrue(com.jobsearch.source.SearchCriteria.isNationwide("Deutschland"));
        assertFalse(com.jobsearch.source.SearchCriteria.isNationwide("Berlin"));
    }

    @Test
    void normalizesForDuplicates() {
        assertEquals(TextUtils.normalizeKey("Senior Java Developer (m/w/d)"),
                TextUtils.normalizeKey("Senior Java Developer"));
        assertEquals("beispiel tech", TextUtils.normalizeKey("Beispiel Tech GmbH"));
    }
}
