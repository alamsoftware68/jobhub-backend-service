package com.jobsearch.web;

import com.jobsearch.domain.Job;
import com.jobsearch.repository.JobRepository;
import com.jobsearch.service.JobIngestionService;
import com.jobsearch.service.JobQueryService;
import com.jobsearch.web.dto.JobDtos.JobDetail;
import com.jobsearch.web.dto.JobDtos.JobSummary;
import com.jobsearch.web.dto.JobDtos.JobUpdateRequest;
import com.jobsearch.web.dto.JobDtos.PageResponse;
import com.jobsearch.web.dto.JobDtos.Stats;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/jobs")
public class JobController {

    private final JobQueryService queries;
    private final JobRepository jobs;
    private final JobIngestionService ingestion;

    public JobController(JobQueryService queries, JobRepository jobs, JobIngestionService ingestion) {
        this.queries = queries;
        this.jobs = jobs;
        this.ingestion = ingestion;
    }

    @GetMapping
    public PageResponse<JobSummary> search(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String source,
            @RequestParam(required = false) String location,
            @RequestParam(required = false) String country,
            @RequestParam(defaultValue = "false") boolean remoteOnly,
            @RequestParam(defaultValue = "0") int minScore,
            @RequestParam(defaultValue = "false") boolean savedOnly,
            @RequestParam(defaultValue = "false") boolean showHidden,
            @RequestParam(required = false) Integer postedWithinDays,
            @RequestParam(defaultValue = "false") boolean englishOnly,
            @RequestParam(defaultValue = "newest") String sort,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        JobQueryService.Filter filter = new JobQueryService.Filter(q, source, location, country, remoteOnly, minScore,
                savedOnly, showHidden, postedWithinDays, englishOnly, sort);
        Page<Job> result = queries.search(filter, page, size);
        return new PageResponse<>(result.map(JobSummary::from).getContent(), result.getNumber(), result.getSize(),
                result.getTotalElements(), result.getTotalPages());
    }

    @GetMapping("/{id}")
    public JobDetail get(@PathVariable Long id) {
        ingestion.loadDetailsIfMissing(id); // Arbeitsagentur: load full description on first open
        return JobDetail.from(find(id));
    }

    @PatchMapping("/{id}")
    @Transactional
    public JobSummary update(@PathVariable Long id, @RequestBody JobUpdateRequest request) {
        Job job = find(id);
        if (request.saved() != null) {
            job.setSaved(request.saved());
        }
        if (request.hidden() != null) {
            job.setHidden(request.hidden());
        }
        return JobSummary.from(jobs.save(job));
    }

    /** Countries present in the stored jobs, for the Country filter. */
    @GetMapping("/countries")
    public List<String> countries() {
        return jobs.findDistinctCountries();
    }

    @GetMapping("/stats")
    public Stats stats() {
        Map<String, Long> perSource = new LinkedHashMap<>();
        ingestion.getStatus().forEach(s -> perSource.put(s.name(), jobs.countBySource(s.id())));
        return new Stats(
                jobs.count(),
                jobs.countByFetchedAtAfter(Instant.now().minus(Duration.ofHours(24))),
                jobs.countBySaved(true),
                perSource,
                ingestion.getLastRunAt(),
                ingestion.isRunning());
    }

    private Job find(Long id) {
        return jobs.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Job not found"));
    }
}
