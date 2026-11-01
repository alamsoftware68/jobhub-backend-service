package com.jobsearch.service;

import com.jobsearch.domain.Job;
import com.jobsearch.repository.JobRepository;
import com.jobsearch.util.Countries;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Search + filters for the Job Search page. */
@Service
public class JobQueryService {

    public record Filter(
            String q,
            String source,
            String location,
            String country,
            boolean remoteOnly,
            int minScore,
            boolean savedOnly,
            boolean showHidden,
            Integer postedWithinDays,
            boolean englishOnly,
            String sort) {
    }

    private final JobRepository jobs;

    public JobQueryService(JobRepository jobs) {
        this.jobs = jobs;
    }

    public Page<Job> search(Filter f, int page, int size) {
        // "score": best match first. Default "newest": newest day first, best match first within each day.
        Sort sort = "score".equals(f.sort())
                ? Sort.by(Sort.Order.desc("matchScore"), Sort.Order.desc("postedAt"))
                : Sort.by(Sort.Order.desc("postedDay"), Sort.Order.desc("matchScore"), Sort.Order.desc("postedAt"));
        return jobs.findAll(toSpec(f), PageRequest.of(Math.max(0, page), Math.min(100, Math.max(1, size)), sort));
    }

    private static Specification<Job> toSpec(Filter f) {
        return (root, query, cb) -> {
            List<Predicate> p = new ArrayList<>();
            if (f.q() != null && !f.q().isBlank()) {
                String like = "%" + f.q().toLowerCase(Locale.ROOT).strip() + "%";
                p.add(cb.or(
                        cb.like(cb.lower(root.<String>get("title")), like),
                        cb.like(cb.lower(root.<String>get("company")), like),
                        cb.like(cb.lower(root.<String>get("description")), like)));
            }
            if (f.source() != null && !f.source().isBlank()) {
                p.add(cb.equal(root.<String>get("source"), f.source()));
            }
            if (f.location() != null && !f.location().isBlank()) {
                List<Predicate> any = new ArrayList<>();
                for (String variant : Countries.cityVariants(f.location())) {
                    any.add(cb.like(cb.lower(root.<String>get("location")), "%" + variant + "%"));
                }
                p.add(cb.or(any.toArray(Predicate[]::new)));
            }
            if (f.country() != null && !f.country().isBlank()) {
                String country = Countries.normalize(f.country());
                p.add(cb.or(
                        cb.equal(root.<String>get("country"), country),
                        cb.like(cb.lower(root.<String>get("location")), "%" + f.country().toLowerCase(Locale.ROOT).strip() + "%")));
            }
            if (f.remoteOnly()) {
                p.add(cb.isTrue(root.<Boolean>get("remote")));
            }
            if (f.minScore() > 0) {
                p.add(cb.greaterThanOrEqualTo(root.<Integer>get("matchScore"), f.minScore()));
            }
            if (f.savedOnly()) {
                p.add(cb.isTrue(root.<Boolean>get("saved")));
            }
            if (!f.showHidden()) {
                p.add(cb.isFalse(root.<Boolean>get("hidden")));
            }
            if (f.postedWithinDays() != null && f.postedWithinDays() > 0) {
                Instant since = Instant.now().minus(Duration.ofDays(f.postedWithinDays()));
                p.add(cb.greaterThanOrEqualTo(root.<Instant>get("postedAt"), since));
            }
            if (f.englishOnly()) {
                p.add(cb.isFalse(root.<Boolean>get("germanRequired")));
            }
            return cb.and(p.toArray(Predicate[]::new));
        };
    }
}
