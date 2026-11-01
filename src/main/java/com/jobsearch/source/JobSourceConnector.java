package com.jobsearch.source;

import java.util.List;

/**
 * One implementation per job site. To add a new source, implement this interface
 * and annotate the class with {@code @Component} — it is picked up automatically.
 */
public interface JobSourceConnector {

    /** Stable id stored in the database, e.g. "ARBEITSAGENTUR". */
    String id();

    /** Name shown in the UI. */
    String displayName();

    /** False when switched off or an API key is missing. */
    boolean isEnabled();

    /** Fetches jobs matching the criteria. Should not throw for single failed requests. */
    List<JobPosting> fetch(SearchCriteria criteria);

    /** Loads the full description for one job, if the source needs a separate call. */
    default String fetchDescription(String externalId) {
        return null;
    }
}
