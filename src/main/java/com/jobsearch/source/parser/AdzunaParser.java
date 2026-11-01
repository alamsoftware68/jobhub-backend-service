package com.jobsearch.source.parser;

import com.fasterxml.jackson.databind.JsonNode;
import com.jobsearch.source.JobPosting;
import com.jobsearch.util.Countries;
import com.jobsearch.util.TextUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Parses the Adzuna search API (https://developer.adzuna.com, free key). */
public final class AdzunaParser {

    public static final String SOURCE = "ADZUNA";

    private AdzunaParser() {
    }

    public static List<JobPosting> parse(JsonNode root) {
        List<JobPosting> result = new ArrayList<>();
        if (root == null) {
            return result;
        }
        for (JsonNode job : root.path("results")) {
            String title = TextUtils.stripHtml(ArbeitsagenturParser.text(job, "title"));
            String description = TextUtils.stripHtml(ArbeitsagenturParser.text(job, "description"));
            String text = ((title == null ? "" : title) + " " + (description == null ? "" : description))
                    .toLowerCase(Locale.ROOT);

            result.add(new JobPosting(
                    SOURCE,
                    ArbeitsagenturParser.text(job, "id"),
                    title,
                    ArbeitsagenturParser.text(job.path("company"), "display_name"),
                    ArbeitsagenturParser.text(job.path("location"), "display_name"),
                    country(job),
                    text.contains("remote") || text.contains("home office") ? Boolean.TRUE : null,
                    description,
                    ArbeitsagenturParser.text(job, "redirect_url"),
                    JsonDates.parse(ArbeitsagenturParser.text(job, "created")),
                    List.of(),
                    salary(job),
                    joinTypes(ArbeitsagenturParser.text(job, "contract_time"),
                            ArbeitsagenturParser.text(job, "contract_type"))));
        }
        return result;
    }

    private static String country(JsonNode job) {
        JsonNode area = job.path("location").path("area");
        String first = area.isArray() && !area.isEmpty() ? area.get(0).asText() : null;
        String known = Countries.knownCountry(first);
        return known != null ? known
                : Countries.fromLocation(ArbeitsagenturParser.text(job.path("location"), "display_name"));
    }

    private static String salary(JsonNode job) {
        double min = job.path("salary_min").asDouble(0);
        double max = job.path("salary_max").asDouble(0);
        if (min <= 0 && max <= 0) {
            return null;
        }
        boolean predicted = "1".equals(job.path("salary_is_predicted").asText());
        String range = min > 0 && max > 0 && min != max
                ? String.format(Locale.GERMANY, "€%,.0f – €%,.0f", min, max)
                : String.format(Locale.GERMANY, "€%,.0f", Math.max(min, max));
        return predicted ? range + " (estimated)" : range;
    }

    private static String joinTypes(String a, String b) {
        if (a == null) {
            return b == null ? null : b.replace('_', ' ');
        }
        return b == null ? a.replace('_', ' ') : (a + ", " + b).replace('_', ' ');
    }
}
