package com.jobsearch.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/** Typed view of the "app:" section in application.yml. */
@ConfigurationProperties(prefix = "app")
public record AppProperties(Cors cors, Fetch fetch, Sources sources) {

    public record Cors(List<String> allowedOrigins) {
    }

    public record Fetch(
            String cron,
            boolean runOnStartup,
            int maxDaysOld,
            int maxResultsPerSearch,
            int cleanupAfterDays,
            int detailsPerRun) {
    }

    public record Sources(Arbeitsagentur arbeitsagentur, Arbeitnow arbeitnow, Adzuna adzuna, Jooble jooble) {
    }

    public record Arbeitsagentur(boolean enabled, String baseUrl, String apiKey, String searchPath, String detailsPath) {
        public Arbeitsagentur {
            searchPath = searchPath == null || searchPath.isBlank() ? "/pc/v6/jobs" : searchPath;
            detailsPath = detailsPath == null || detailsPath.isBlank() ? "/pc/v4/jobdetails" : detailsPath;
        }
    }

    public record Arbeitnow(boolean enabled, String baseUrl, int maxPages) {
    }

    public record Adzuna(String appId, String appKey, String baseUrl) {
        public boolean enabled() {
            return appId != null && !appId.isBlank() && appKey != null && !appKey.isBlank();
        }
    }

    public record Jooble(String apiKey, String baseUrl) {
        public boolean enabled() {
            return apiKey != null && !apiKey.isBlank();
        }
    }
}
