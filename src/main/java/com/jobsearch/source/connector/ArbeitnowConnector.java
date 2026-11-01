package com.jobsearch.source.connector;

import com.fasterxml.jackson.databind.JsonNode;
import com.jobsearch.config.AppProperties;
import com.jobsearch.service.MatchScorer;
import com.jobsearch.source.JobPosting;
import com.jobsearch.source.JobSourceConnector;
import com.jobsearch.source.SearchCriteria;
import com.jobsearch.source.parser.ArbeitnowParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Arbeitnow — Germany/Europe job board, many English-speaking tech jobs. Free, no key.
 * The API is a feed without keyword search, so jobs are filtered here by your keywords.
 */
@Component
public class ArbeitnowConnector implements JobSourceConnector {

    private static final Logger log = LoggerFactory.getLogger(ArbeitnowConnector.class);
    private static final Set<String> GENERIC = Set.of(
            "developer", "engineer", "entwickler", "senior", "junior", "software", "lead", "full", "stack");

    private final RestClient http;
    private final AppProperties.Arbeitnow config;

    public ArbeitnowConnector(RestClient jobApiClient, AppProperties props) {
        this.http = jobApiClient;
        this.config = props.sources().arbeitnow();
    }

    @Override
    public String id() {
        return ArbeitnowParser.SOURCE;
    }

    @Override
    public String displayName() {
        return "Arbeitnow";
    }

    @Override
    public boolean isEnabled() {
        return config != null && config.enabled();
    }

    @Override
    public List<JobPosting> fetch(SearchCriteria criteria) {
        List<String> terms = significantTerms(criteria.keywords());
        Instant oldest = Instant.now().minus(Duration.ofDays(criteria.maxDaysOld()));
        List<JobPosting> result = new ArrayList<>();

        for (int page = 1; page <= Math.max(1, config.maxPages()); page++) {
            try {
                JsonNode body = http.get().uri(config.baseUrl() + "?page={page}", page)
                        .retrieve()
                        .body(JsonNode.class);
                for (JobPosting job : ArbeitnowParser.parse(body)) {
                    boolean recent = job.postedAt() == null || job.postedAt().isAfter(oldest);
                    if (recent && matchesAny(job, terms) && matchesPlace(job, criteria)) {
                        result.add(job);
                    }
                }
                if (!ArbeitnowParser.hasNextPage(body)) {
                    break;
                }
            } catch (Exception e) {
                log.warn("Arbeitnow page {} failed: {}", page, e.getMessage());
                break;
            }
        }
        return result;
    }

    /** "Java Developer" -> ["java"]; "Spring Boot" -> ["spring boot"]. */
    static List<String> significantTerms(List<String> keywords) {
        List<String> terms = new ArrayList<>();
        for (String keyword : keywords) {
            List<String> words = new ArrayList<>();
            for (String w : keyword.toLowerCase(Locale.ROOT).split("\\s+")) {
                if (!w.isBlank() && !GENERIC.contains(w)) {
                    words.add(w);
                }
            }
            if (!words.isEmpty()) {
                terms.add(String.join(" ", words));
            }
        }
        return terms;
    }

    /**
     * Arbeitnow has no location search, so jobs are filtered by country here. A job whose country
     * can't be recognised is kept for Germany, since the board is mostly German.
     */
    private static boolean matchesPlace(JobPosting job, SearchCriteria criteria) {
        if (job.country() == null) {
            return criteria.isGermany();
        }
        return job.country().equals(criteria.country());
    }

    private static boolean matchesAny(JobPosting job, List<String> terms) {
        if (terms.isEmpty()) {
            return true;
        }
        String text = (job.title() + " " + String.join(" ", job.tags())).toLowerCase(Locale.ROOT);
        for (String term : terms) {
            if (MatchScorer.containsTerm(text, term)) {
                return true;
            }
        }
        return false;
    }
}
