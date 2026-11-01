package com.jobsearch.service;

import com.jobsearch.domain.Job;
import com.jobsearch.repository.JobRepository;
import com.jobsearch.source.JobPosting;
import com.jobsearch.util.Countries;
import com.jobsearch.util.TextUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

/** Inserts or updates one posting, merging duplicates found on other sources. */
@Service
public class JobUpsertService {

    public enum Outcome { NEW, UPDATED, DUPLICATE }

    public record UpsertResult(Outcome outcome, Long jobId) {
    }

    private final JobRepository jobs;
    private final JobScoringService scoring;

    public JobUpsertService(JobRepository jobs, JobScoringService scoring) {
        this.jobs = jobs;
        this.scoring = scoring;
    }

    @Transactional
    public UpsertResult upsert(JobPosting posting, MatchScorer.ProfileSnapshot profile) {
        Instant now = Instant.now();

        Optional<Job> existing = jobs.findBySourceAndExternalId(posting.source(), posting.externalId());
        if (existing.isPresent()) {
            Job job = existing.get();
            copyFields(posting, job);
            job.setFetchedAt(now);
            scoring.apply(job, profile);
            jobs.save(job);
            return new UpsertResult(Outcome.UPDATED, job.getId());
        }

        String dedupKey = dedupKey(posting);
        List<Job> sameJob = jobs.findByDedupKey(dedupKey).stream()
                .filter(j -> !j.getSource().equals(posting.source()))
                .toList();
        if (!sameJob.isEmpty()) {
            Job primary = sameJob.get(0);
            String line = posting.source() + "|" + posting.url();
            String others = primary.getOtherSources() == null ? "" : primary.getOtherSources();
            if (!others.contains(posting.source() + "|")) {
                primary.setOtherSources(others.isEmpty() ? line : others + "\n" + line);
            }
            if (TextUtils.isBlank(primary.getDescription()) || isShort(primary.getDescription())) {
                if (!TextUtils.isBlank(posting.description()) && !isShort(posting.description())) {
                    primary.setDescription(posting.description());
                    scoring.apply(primary, profile);
                }
            }
            primary.setFetchedAt(now);
            jobs.save(primary);
            return new UpsertResult(Outcome.DUPLICATE, primary.getId());
        }

        Job job = new Job();
        job.setSource(posting.source());
        job.setExternalId(posting.externalId());
        job.setDedupKey(dedupKey);
        copyFields(posting, job);
        job.setFetchedAt(now);
        scoring.apply(job, profile);
        jobs.save(job);
        return new UpsertResult(Outcome.NEW, job.getId());
    }

    /** Stores a description loaded later (Arbeitsagentur details) and re-scores the job. */
    @Transactional
    public void applyDescription(Long jobId, String description, MatchScorer.ProfileSnapshot profile) {
        jobs.findById(jobId).ifPresent(job -> {
            if (!TextUtils.isBlank(description)) {
                job.setDescription(description);
            }
            job.setDetailsFetched(true);
            scoring.apply(job, profile);
            jobs.save(job);
        });
    }

    private static void copyFields(JobPosting p, Job job) {
        job.setTitle(TextUtils.truncate(p.title(), 500));
        job.setCompany(TextUtils.truncate(p.company(), 300));
        job.setLocation(TextUtils.truncate(p.location(), 300));
        String country = p.country() != null ? p.country() : Countries.fromLocation(p.location());
        if (country != null || job.getCountry() == null) {
            job.setCountry(country);
        }
        job.setRemote(p.remote());
        job.setUrl(TextUtils.truncate(p.url(), 2000));
        if (p.postedAt() != null) {
            job.setPostedAt(p.postedAt());
        } else if (job.getPostedAt() == null) {
            job.setPostedAt(Instant.now()); // unknown date: treat as "seen today"
        }
        job.setPostedDay(LocalDate.ofInstant(job.getPostedAt(), ZoneOffset.UTC));
        job.setTags(TextUtils.joinCsv(p.tags().stream().map(t -> t.replace(",", " ")).toList()));
        job.setSalary(TextUtils.truncate(p.salary(), 200));
        job.setEmploymentType(TextUtils.truncate(p.employmentType(), 200));
        // Don't overwrite a full description (loaded via details) with a short search snippet.
        if (!TextUtils.isBlank(p.description())
                && (TextUtils.isBlank(job.getDescription()) || !job.isDetailsFetched())) {
            job.setDescription(p.description());
        }
    }

    static String dedupKey(JobPosting p) {
        String city = p.location() == null ? "" : p.location().split("[,/(]")[0];
        String key = TextUtils.normalizeKey(p.company()) + "|" + TextUtils.normalizeKey(p.title()) + "|"
                + TextUtils.normalizeKey(city);
        return TextUtils.truncate(key, 600);
    }

    private static boolean isShort(String text) {
        return text.length() < 200;
    }
}
