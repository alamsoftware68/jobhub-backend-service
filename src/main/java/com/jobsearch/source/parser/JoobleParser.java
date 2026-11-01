package com.jobsearch.source.parser;

import com.fasterxml.jackson.databind.JsonNode;
import com.jobsearch.source.JobPosting;
import com.jobsearch.util.Countries;
import com.jobsearch.util.TextUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Parses the Jooble REST API (https://jooble.org/api/about, free key). */
public final class JoobleParser {

    public static final String SOURCE = "JOOBLE";

    private JoobleParser() {
    }

    public static List<JobPosting> parse(JsonNode root) {
        List<JobPosting> result = new ArrayList<>();
        if (root == null) {
            return result;
        }
        for (JsonNode job : root.path("jobs")) {
            String title = TextUtils.stripHtml(ArbeitsagenturParser.text(job, "title"));
            String snippet = TextUtils.stripHtml(ArbeitsagenturParser.text(job, "snippet"));
            String location = ArbeitsagenturParser.text(job, "location");
            String text = ((title == null ? "" : title) + " " + (location == null ? "" : location))
                    .toLowerCase(Locale.ROOT);
            String origin = ArbeitsagenturParser.text(job, "source");

            result.add(new JobPosting(
                    SOURCE,
                    ArbeitsagenturParser.text(job, "id"),
                    title,
                    ArbeitsagenturParser.text(job, "company"),
                    location,
                    Countries.fromLocation(location),
                    text.contains("remote") || text.contains("home office") ? Boolean.TRUE : null,
                    snippet,
                    ArbeitsagenturParser.text(job, "link"),
                    JsonDates.parse(ArbeitsagenturParser.text(job, "updated")),
                    origin == null ? List.of() : List.of("via " + origin),
                    ArbeitsagenturParser.text(job, "salary"),
                    ArbeitsagenturParser.text(job, "type")));
        }
        return result;
    }
}
