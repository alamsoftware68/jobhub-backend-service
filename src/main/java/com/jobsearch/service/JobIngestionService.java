package com.jobsearch.service;

import com.jobsearch.config.AppProperties;
import com.jobsearch.repository.JobRepository;
import com.jobsearch.source.JobPosting;
import com.jobsearch.source.JobSourceConnector;
import com.jobsearch.source.SearchCriteria;
import com.jobsearch.source.parser.ArbeitsagenturParser;
import com.jobsearch.util.Countries;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/** Pulls jobs from every enabled source on a schedule and stores them. */
@Service
public class JobIngestionService {

    private static final Logger log = LoggerFactory.getLogger(JobIngestionService.class);

    /** Status of the last run per source, shown on the UI. */
    public record SourceStatus(
            String id,
            String name,
            boolean enabled,
            Instant lastRunAt,
            int fetched,
            int newJobs,
            int duplicates,
            String lastError) {
    }

    private final List<JobSourceConnector> connectors;
    private final JobUpsertService upserts;
    private final ProfileService profiles;
    private final JobRepository jobs;
    private final AppProperties.Fetch fetchConfig;
    private final TransactionTemplate tx;

    private final AtomicBoolean running = new AtomicBoolean(false);
    private final Map<String, SourceStatus> status = new ConcurrentHashMap<>();
    private volatile Instant lastRunAt;

    public JobIngestionService(List<JobSourceConnector> connectors,
                               JobUpsertService upserts,
                               ProfileService profiles,
                               JobRepository jobs,
                               AppProperties props,
                               TransactionTemplate tx) {
        this.connectors = connectors;
        this.upserts = upserts;
        this.profiles = profiles;
        this.jobs = jobs;
        this.fetchConfig = props.fetch();
        this.tx = tx;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onStartup() {
        if (fetchConfig.runOnStartup()) {
            triggerAsync();
        }
    }

    @Scheduled(cron = "${app.fetch.cron}")
    public void scheduledRun() {
        runAll();
    }

    /** A one-off search for another city / country, started from the Jobs page. */
    public record LocationSearch(String keyword, String city, String country) {
    }

    /** Starts a run in the background. Returns false if one is already running. */
    public boolean triggerAsync() {
        return triggerAsync(null);
    }

    /** Starts a run in the background, optionally for a specific city/country. */
    public boolean triggerAsync(LocationSearch override) {
        if (running.get()) {
            return false;
        }
        CompletableFuture.runAsync(() -> runAll(override));
        return true;
    }

    public boolean isRunning() {
        return running.get();
    }

    public Instant getLastRunAt() {
        return lastRunAt;
    }

    public List<SourceStatus> getStatus() {
        List<SourceStatus> list = new ArrayList<>();
        for (JobSourceConnector c : connectors) {
            list.add(status.getOrDefault(c.id(),
                    new SourceStatus(c.id(), c.displayName(), c.isEnabled(), null, 0, 0, 0, null)));
        }
        return list;
    }

    public void runAll() {
        runAll(null);
    }

    public void runAll(LocationSearch override) {
        if (!running.compareAndSet(false, true)) {
            log.info("Fetch already running — skipped");
            return;
        }
        try {
            MatchScorer.ProfileSnapshot profile = profiles.snapshot();
            List<String> keywords = profile.searchKeywords().isEmpty() ? List.of("Java Developer") : profile.searchKeywords();
            List<String> locations = profile.locations();
            String country = null;
            if (override != null) {
                if (override.keyword() != null && !override.keyword().isBlank()) {
                    keywords = List.of(override.keyword().strip());
                }
                boolean hasCity = override.city() != null && !override.city().isBlank();
                boolean hasCountry = override.country() != null && !override.country().isBlank();
                if (hasCity || hasCountry) {
                    locations = hasCity ? List.of(override.city().strip()) : List.of();
                    country = hasCountry ? override.country() : Countries.fromLocation(override.city());
                }
                log.info("Location search: keywords={}, city={}, country={}", keywords, locations, country);
            }
            SearchCriteria criteria = new SearchCriteria(
                    keywords,
                    locations,
                    country,
                    profiles.get().getRadiusKm(),
                    fetchConfig.maxDaysOld(),
                    fetchConfig.maxResultsPerSearch());

            for (JobSourceConnector connector : connectors) {
                if (!connector.isEnabled()) {
                    status.put(connector.id(), new SourceStatus(connector.id(), connector.displayName(),
                            false, null, 0, 0, 0, null));
                    continue;
                }
                runOne(connector, criteria, profile);
            }
            cleanup();
            lastRunAt = Instant.now();
        } finally {
            running.set(false);
        }
    }

    private void runOne(JobSourceConnector connector, SearchCriteria criteria, MatchScorer.ProfileSnapshot profile) {
        long start = System.currentTimeMillis();
        int fetched = 0;
        int newJobs = 0;
        int duplicates = 0;
        String error = null;
        List<Long> needDetails = new ArrayList<>();
        try {
            List<JobPosting> postings = connector.fetch(criteria);
            Set<String> seen = new HashSet<>();
            for (JobPosting posting : postings) {
                if (!posting.isValid() || !seen.add(posting.externalId())) {
                    continue;
                }
                fetched++;
                try {
                    JobUpsertService.UpsertResult r = upserts.upsert(posting, profile);
                    if (r.outcome() == JobUpsertService.Outcome.NEW) {
                        newJobs++;
                        if (ArbeitsagenturParser.SOURCE.equals(connector.id())) {
                            needDetails.add(r.jobId());
                        }
                    } else if (r.outcome() == JobUpsertService.Outcome.DUPLICATE) {
                        duplicates++;
                    }
                } catch (Exception e) {
                    log.warn("Could not store job {} from {}: {}", posting.externalId(), connector.id(), e.getMessage());
                }
            }
            loadDetails(connector, needDetails, profile);
        } catch (Exception e) {
            error = e.getMessage();
            log.error("Source {} failed", connector.id(), e);
        }
        status.put(connector.id(), new SourceStatus(connector.id(), connector.displayName(), true,
                Instant.now(), fetched, newJobs, duplicates, error));
        log.info("{}: {} fetched, {} new, {} duplicates ({} ms)", connector.displayName(), fetched, newJobs,
                duplicates, System.currentTimeMillis() - start);
    }

    /** Arbeitsagentur search results have no description — load it for new jobs (limited per run). */
    private void loadDetails(JobSourceConnector connector, List<Long> jobIds, MatchScorer.ProfileSnapshot profile) {
        int limit = Math.min(jobIds.size(), Math.max(0, fetchConfig.detailsPerRun()));
        for (int i = 0; i < limit; i++) {
            Long id = jobIds.get(i);
            jobs.findById(id).ifPresent(job -> {
                String description = connector.fetchDescription(job.getExternalId());
                upserts.applyDescription(id, description, profile);
            });
            sleepQuietly(150); // be polite to the public API
        }
    }

    /** Loads the description for a single job on demand (when opened in the UI). */
    public void loadDetailsIfMissing(Long jobId) {
        jobs.findById(jobId).ifPresent(job -> {
            if (job.isDetailsFetched() || !ArbeitsagenturParser.SOURCE.equals(job.getSource())) {
                return;
            }
            connectors.stream()
                    .filter(c -> c.id().equals(job.getSource()))
                    .findFirst()
                    .ifPresent(c -> upserts.applyDescription(jobId, c.fetchDescription(job.getExternalId()),
                            profiles.snapshot()));
        });
    }

    private void cleanup() {
        if (fetchConfig.cleanupAfterDays() <= 0) {
            return;
        }
        Instant cutoff = Instant.now().minus(Duration.ofDays(fetchConfig.cleanupAfterDays()));
        Integer deleted = tx.execute(s -> jobs.deleteUnsavedOlderThan(cutoff));
        if (deleted != null && deleted > 0) {
            log.info("Removed {} old jobs", deleted);
        }
    }

    private static void sleepQuietly(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
