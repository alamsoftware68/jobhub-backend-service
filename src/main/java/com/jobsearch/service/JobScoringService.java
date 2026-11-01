package com.jobsearch.service;

import com.jobsearch.domain.Job;
import com.jobsearch.repository.JobRepository;
import com.jobsearch.util.LanguageDetector;
import com.jobsearch.util.TextUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Applies language detection and the match score to jobs. */
@Service
public class JobScoringService {

    private static final Logger log = LoggerFactory.getLogger(JobScoringService.class);

    private final MatchScorer scorer;
    private final JobRepository jobs;
    private final ProfileService profiles;

    public JobScoringService(MatchScorer scorer, JobRepository jobs, ProfileService profiles) {
        this.scorer = scorer;
        this.jobs = jobs;
        this.profiles = profiles;
    }

    public void apply(Job job, MatchScorer.ProfileSnapshot profile) {
        String text = job.getTitle() + "\n" + (job.getDescription() == null ? "" : job.getDescription());
        String language = LanguageDetector.detect(text);
        job.setLanguage(language);
        job.setGermanRequired(LanguageDetector.germanRequired(text, language));

        MatchScorer.Result r = scorer.score(new MatchScorer.JobInput(
                job.getTitle(),
                job.getDescription(),
                TextUtils.splitCsv(job.getTags()),
                job.getLocation(),
                job.getRemote(),
                language,
                job.isGermanRequired()), profile);
        job.setMatchScore(r.score());
        job.setMatchedSkills(TextUtils.joinCsv(r.matchedSkills()));
        job.setMissingSkills(TextUtils.joinCsv(r.missingSkills()));
    }

    /** Recalculates every stored job, e.g. after the profile changed. */
    @Transactional
    public int rescoreAll() {
        MatchScorer.ProfileSnapshot profile = profiles.snapshot();
        int count = 0;
        int page = 0;
        Page<Job> batch;
        do {
            batch = jobs.findAll(PageRequest.of(page++, 500, Sort.by("id")));
            for (Job job : batch) {
                apply(job, profile);
                count++;
            }
            jobs.saveAll(batch.getContent());
        } while (batch.hasNext());
        log.info("Re-scored {} jobs", count);
        return count;
    }
}
