package com.jobsearch.source;

import java.time.Instant;
import java.util.List;

/** A job as returned by any source, already converted to one common format. */
public record JobPosting(
        String source,
        String externalId,
        String title,
        String company,
        String location,
        String country,          // English country name, e.g. "Germany"; null if unknown
        Boolean remote,
        String description,
        String url,
        Instant postedAt,
        List<String> tags,
        String salary,
        String employmentType) {

    public JobPosting {
        tags = tags == null ? List.of() : List.copyOf(tags);
    }

    public boolean isValid() {
        return externalId != null && !externalId.isBlank()
                && title != null && !title.isBlank()
                && url != null && !url.isBlank();
    }
}
