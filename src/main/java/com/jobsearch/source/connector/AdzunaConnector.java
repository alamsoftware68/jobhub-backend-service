package com.jobsearch.source.connector;

import com.fasterxml.jackson.databind.JsonNode;
import com.jobsearch.config.AppProperties;
import com.jobsearch.source.JobPosting;
import com.jobsearch.source.JobSourceConnector;
import com.jobsearch.source.SearchCriteria;
import com.jobsearch.source.parser.AdzunaParser;
import com.jobsearch.util.Countries;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.ArrayList;
import java.util.List;

/** Adzuna aggregator (many countries). Needs a free app id + key from developer.adzuna.com. */
@Component
public class AdzunaConnector implements JobSourceConnector {

    private static final Logger log = LoggerFactory.getLogger(AdzunaConnector.class);
    private static final int PAGE_SIZE = 50;

    private final RestClient http;
    private final AppProperties.Adzuna config;

    public AdzunaConnector(RestClient jobApiClient, AppProperties props) {
        this.http = jobApiClient;
        this.config = props.sources().adzuna();
    }

    @Override
    public String id() {
        return AdzunaParser.SOURCE;
    }

    @Override
    public String displayName() {
        return "Adzuna";
    }

    @Override
    public boolean isEnabled() {
        return config != null && config.enabled();
    }

    @Override
    public List<JobPosting> fetch(SearchCriteria criteria) {
        List<JobPosting> all = new ArrayList<>();
        String countryCode = Countries.adzunaCode(criteria.country());
        if (countryCode == null) {
            log.info("Adzuna does not cover {}", criteria.country());
            return all;
        }
        List<String> locations = criteria.locations().isEmpty() ? List.of("Deutschland") : criteria.locations();
        int pages = Math.max(1, (int) Math.ceil(criteria.maxResultsPerSearch() / (double) PAGE_SIZE));

        for (String keyword : criteria.keywords()) {
            for (String location : locations) {
                for (int page = 1; page <= pages; page++) {
                    UriComponentsBuilder uri = UriComponentsBuilder.fromUriString(config.baseUrl() + "/" + countryCode + "/search/" + page)
                            .queryParam("app_id", config.appId())
                            .queryParam("app_key", config.appKey())
                            .queryParam("what", keyword)
                            .queryParam("results_per_page", PAGE_SIZE)
                            .queryParam("max_days_old", criteria.maxDaysOld());
                    if (!SearchCriteria.isNationwide(location)) {
                        uri.queryParam("where", location).queryParam("distance", criteria.radiusKm());
                    }
                    try {
                        JsonNode body = http.get().uri(uri.encode().build().toUri()).retrieve().body(JsonNode.class);
                        List<JobPosting> jobs = AdzunaParser.parse(body);
                        for (JobPosting j : jobs) {
                            all.add(j.country() != null ? j : withCountry(j, criteria.country()));
                        }
                        if (jobs.size() < PAGE_SIZE) {
                            break;
                        }
                    } catch (Exception e) {
                        log.warn("Adzuna search failed for '{}' in '{}': {}", keyword, location, e.getMessage());
                        break;
                    }
                }
            }
        }
        return all;
    }

    private static JobPosting withCountry(JobPosting j, String country) {
        return new JobPosting(j.source(), j.externalId(), j.title(), j.company(), j.location(), country, j.remote(),
                j.description(), j.url(), j.postedAt(), j.tags(), j.salary(), j.employmentType());
    }
}
