package com.jobsearch.web.dto;

import com.jobsearch.domain.Job;
import com.jobsearch.source.SourceNames;
import com.jobsearch.util.TextUtils;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Request/response shapes for the jobs API. */
public final class JobDtos {

    private JobDtos() {
    }

    public record SourceLink(String source, String sourceName, String url) {
    }

    public record JobSummary(
            Long id,
            String source,
            String sourceName,
            String title,
            String company,
            String location,
            String country,
            Boolean remote,
            String snippet,
            String url,
            Instant postedAt,
            LocalDate postedDay,
            int matchScore,
            List<String> matchedSkills,
            String salary,
            String employmentType,
            String language,
            boolean germanRequired,
            boolean saved,
            boolean hidden,
            List<SourceLink> otherSources) {

        public static JobSummary from(Job j) {
            return new JobSummary(j.getId(), j.getSource(), SourceNames.displayName(j.getSource()), j.getTitle(),
                    j.getCompany(), j.getLocation(), j.getCountry(), j.getRemote(),
                    TextUtils.truncate(j.getDescription() == null ? null : j.getDescription().replace('\n', ' '), 260),
                    j.getUrl(), j.getPostedAt(), j.getPostedDay(), j.getMatchScore(), TextUtils.splitCsv(j.getMatchedSkills()),
                    j.getSalary(), j.getEmploymentType(), j.getLanguage(), j.isGermanRequired(), j.isSaved(),
                    j.isHidden(), parseOtherSources(j));
        }
    }

    public record JobDetail(
            JobSummary summary,
            String description,
            List<String> missingSkills,
            List<String> tags,
            Instant fetchedAt) {

        public static JobDetail from(Job j) {
            return new JobDetail(JobSummary.from(j), j.getDescription(), TextUtils.splitCsv(j.getMissingSkills()),
                    TextUtils.splitCsv(j.getTags()), j.getFetchedAt());
        }
    }

    public record JobUpdateRequest(Boolean saved, Boolean hidden) {
    }

    public record PageResponse<T>(List<T> content, int page, int size, long totalElements, int totalPages) {
    }

    public record Stats(
            long totalJobs,
            long newLast24h,
            long savedJobs,
            Map<String, Long> perSource,
            Instant lastRunAt,
            boolean fetchRunning) {
    }

    static List<SourceLink> parseOtherSources(Job j) {
        List<SourceLink> links = new ArrayList<>();
        if (j.getOtherSources() == null) {
            return links;
        }
        for (String line : j.getOtherSources().split("\n")) {
            int sep = line.indexOf('|');
            if (sep > 0) {
                String source = line.substring(0, sep);
                links.add(new SourceLink(source, SourceNames.displayName(source), line.substring(sep + 1)));
            }
        }
        return links;
    }
}
