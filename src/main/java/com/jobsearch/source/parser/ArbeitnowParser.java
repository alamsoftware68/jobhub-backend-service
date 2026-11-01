package com.jobsearch.source.parser;

import com.fasterxml.jackson.databind.JsonNode;
import com.jobsearch.source.JobPosting;
import com.jobsearch.util.Countries;
import com.jobsearch.util.TextUtils;

import java.util.ArrayList;
import java.util.List;

/** Parses https://www.arbeitnow.com/api/job-board-api (free, no key). */
public final class ArbeitnowParser {

    public static final String SOURCE = "ARBEITNOW";

    private ArbeitnowParser() {
    }

    public static List<JobPosting> parse(JsonNode root) {
        List<JobPosting> result = new ArrayList<>();
        if (root == null) {
            return result;
        }
        for (JsonNode job : root.path("data")) {
            List<String> tags = new ArrayList<>();
            job.path("tags").forEach(t -> tags.add(t.asText()));
            List<String> types = new ArrayList<>();
            job.path("job_types").forEach(t -> types.add(t.asText()));

            JsonNode remoteNode = job.get("remote");
            Boolean remote = remoteNode == null || remoteNode.isNull() ? null : remoteNode.asBoolean();

            result.add(new JobPosting(
                    SOURCE,
                    ArbeitsagenturParser.text(job, "slug"),
                    ArbeitsagenturParser.text(job, "title"),
                    ArbeitsagenturParser.text(job, "company_name"),
                    ArbeitsagenturParser.text(job, "location"),
                    Countries.fromLocation(ArbeitsagenturParser.text(job, "location")),
                    remote,
                    TextUtils.stripHtml(ArbeitsagenturParser.text(job, "description")),
                    ArbeitsagenturParser.text(job, "url"),
                    JsonDates.fromEpochSeconds(job.path("created_at").asLong(0)),
                    tags,
                    null,
                    types.isEmpty() ? null : String.join(", ", types)));
        }
        return result;
    }

    /** True when the response says there is another page. */
    public static boolean hasNextPage(JsonNode root) {
        JsonNode next = root == null ? null : root.path("links").get("next");
        return next != null && !next.isNull() && !next.asText().isBlank();
    }
}
