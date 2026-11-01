package com.jobsearch.source.connector;

import com.fasterxml.jackson.databind.JsonNode;
import com.jobsearch.config.AppProperties;
import com.jobsearch.source.JobPosting;
import com.jobsearch.source.JobSourceConnector;
import com.jobsearch.source.SearchCriteria;
import com.jobsearch.source.parser.JoobleParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Jooble aggregator. Needs a free API key from jooble.org/api/about. */
@Component
public class JoobleConnector implements JobSourceConnector {

    private static final Logger log = LoggerFactory.getLogger(JoobleConnector.class);
    private static final int MAX_PAGES = 2;

    private final RestClient http;
    private final AppProperties.Jooble config;

    public JoobleConnector(RestClient jobApiClient, AppProperties props) {
        this.http = jobApiClient;
        this.config = props.sources().jooble();
    }

    @Override
    public String id() {
        return JoobleParser.SOURCE;
    }

    @Override
    public String displayName() {
        return "Jooble";
    }

    @Override
    public boolean isEnabled() {
        return config != null && config.enabled();
    }

    @Override
    public List<JobPosting> fetch(SearchCriteria criteria) {
        List<JobPosting> all = new ArrayList<>();
        List<String> locations = criteria.locations().isEmpty() ? List.of("Deutschland") : criteria.locations();

        for (String keyword : criteria.keywords()) {
            for (String location : locations) {
                for (int page = 1; page <= MAX_PAGES; page++) {
                    Map<String, Object> request = new LinkedHashMap<>();
                    request.put("keywords", keyword);
                    request.put("location", SearchCriteria.isNationwide(location)
                            ? criteria.country() : location + ", " + criteria.country());
                    if (!SearchCriteria.isNationwide(location)) {
                        request.put("radius", String.valueOf(criteria.radiusKm()));
                    }
                    request.put("page", String.valueOf(page));
                    try {
                        JsonNode body = http.post()
                                .uri(config.baseUrl() + "/{key}", config.apiKey())
                                .contentType(MediaType.APPLICATION_JSON)
                                .body(request)
                                .retrieve()
                                .body(JsonNode.class);
                        List<JobPosting> jobs = JoobleParser.parse(body);
                        all.addAll(jobs);
                        if (jobs.isEmpty()) {
                            break;
                        }
                    } catch (Exception e) {
                        log.warn("Jooble search failed for '{}' in '{}': {}", keyword, location, e.getMessage());
                        break;
                    }
                }
            }
        }
        return all;
    }
}
