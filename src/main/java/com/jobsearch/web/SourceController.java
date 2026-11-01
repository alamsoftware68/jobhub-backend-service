package com.jobsearch.web;

import com.jobsearch.service.JobIngestionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/sources")
public class SourceController {

    private final JobIngestionService ingestion;

    public SourceController(JobIngestionService ingestion) {
        this.ingestion = ingestion;
    }

    @GetMapping
    public List<JobIngestionService.SourceStatus> list() {
        return ingestion.getStatus();
    }

    /**
     * Fetch new jobs now (runs in the background). Optional body to search a specific place:
     * {"city": "Munich", "country": "Germany", "keyword": "Java Developer"}
     */
    @PostMapping("/refresh")
    public ResponseEntity<Map<String, Object>> refresh(
            @RequestBody(required = false) JobIngestionService.LocationSearch search) {
        boolean started = ingestion.triggerAsync(search);
        return ResponseEntity.accepted().body(Map.of(
                "started", started,
                "message", started ? "Fetching new jobs…" : "A fetch is already running"));
    }
}
