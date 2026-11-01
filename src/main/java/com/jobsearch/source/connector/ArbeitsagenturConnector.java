package com.jobsearch.source.connector;

import com.fasterxml.jackson.databind.JsonNode;
import com.jobsearch.config.AppProperties;
import com.jobsearch.source.JobPosting;
import com.jobsearch.source.JobSourceConnector;
import com.jobsearch.source.SearchCriteria;
import com.jobsearch.source.parser.ArbeitsagenturParser;
import com.jobsearch.util.Countries;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

/** Bundesagentur für Arbeit — Germany's official job board. Free public API. */
@Component
public class ArbeitsagenturConnector implements JobSourceConnector {

    private static final Logger log = LoggerFactory.getLogger(ArbeitsagenturConnector.class);
    private static final int PAGE_SIZE = 50;

    private final RestClient http;
    private final AppProperties.Arbeitsagentur config;

    public ArbeitsagenturConnector(RestClient jobApiClient, AppProperties props) {
        this.http = jobApiClient;
        this.config = props.sources().arbeitsagentur();
    }

    @Override
    public String id() {
        return ArbeitsagenturParser.SOURCE;
    }

    @Override
    public String displayName() {
        return "Arbeitsagentur";
    }

    @Override
    public boolean isEnabled() {
        return config != null && config.enabled();
    }

    @Override
    public List<JobPosting> fetch(SearchCriteria criteria) {
        List<JobPosting> all = new ArrayList<>();
        if (!criteria.isGermany()) {
            return all; // the Bundesagentur only covers jobs in Germany
        }
        List<String> locations = criteria.locations().isEmpty() ? List.of("Deutschland") : criteria.locations();
        for (String keyword : criteria.keywords()) {
            for (String location : locations) {
                all.addAll(search(keyword, location, criteria));
            }
        }
        return all;
    }

    private List<JobPosting> search(String keyword, String location, SearchCriteria criteria) {
        List<JobPosting> result = new ArrayList<>();
        int pages = Math.max(1, (int) Math.ceil(criteria.maxResultsPerSearch() / (double) PAGE_SIZE));
        for (int page = 1; page <= pages; page++) {
            UriComponentsBuilder uri = UriComponentsBuilder.fromUriString(config.baseUrl() + config.searchPath())
                    .queryParam("was", keyword)
                    .queryParam("angebotsart", 1)
                    .queryParam("veroeffentlichtseit", Math.min(100, criteria.maxDaysOld()))
                    .queryParam("page", page)
                    .queryParam("size", PAGE_SIZE);
            if (!SearchCriteria.isNationwide(location)) {
                uri.queryParam("wo", Countries.germanCityName(location)).queryParam("umkreis", criteria.radiusKm());
            }
            URI target = uri.encode().build().toUri();
            try {
                JsonNode body = http.get().uri(target)
                        .header("X-API-Key", config.apiKey())
                        .retrieve()
                        .body(JsonNode.class);
                List<JobPosting> jobs = ArbeitsagenturParser.parseSearch(body);
                result.addAll(jobs);
                if (jobs.size() < PAGE_SIZE) {
                    break;
                }
            } catch (Exception e) {
                log.warn("Arbeitsagentur search failed for '{}' in '{}': {}", keyword, location, e.getMessage());
                break;
            }
        }
        return result;
    }

    @Override
    public String fetchDescription(String refnr) {
        try {
            JsonNode body = http.get()
                    .uri(config.baseUrl() + config.detailsPath() + "/{code}", ArbeitsagenturParser.encodeRefnr(refnr))
                    .header("X-API-Key", config.apiKey())
                    .retrieve()
                    .body(JsonNode.class);
            return ArbeitsagenturParser.parseDetailDescription(body);
        } catch (Exception e) {
            log.debug("Could not load Arbeitsagentur details for {}: {}", refnr, e.getMessage());
            return null;
        }
    }
}
