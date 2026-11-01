package com.jobsearch.source.parser;

import com.fasterxml.jackson.databind.JsonNode;
import com.jobsearch.source.JobPosting;
import com.jobsearch.util.Countries;
import com.jobsearch.util.TextUtils;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Parses responses of the Bundesagentur für Arbeit "Jobsuche" API.
 * Search: /pc/v6/jobs (list in "ergebnisliste"). Older v4 format ("stellenangebote") is still understood.
 * Details: /pc/v4/jobdetails/{base64(refnr)}.
 */
public final class ArbeitsagenturParser {

    public static final String SOURCE = "ARBEITSAGENTUR";
    private static final String JOB_PAGE = "https://www.arbeitsagentur.de/jobsuche/jobdetail/";

    private ArbeitsagenturParser() {
    }

    public static List<JobPosting> parseSearch(JsonNode root) {
        List<JobPosting> result = new ArrayList<>();
        if (root == null) {
            return result;
        }
        JsonNode list = root.has("ergebnisliste") ? root.path("ergebnisliste") : root.path("stellenangebote");
        for (JsonNode job : list) {
            String refnr = firstText(job, "referenznummer", "refnr");
            if (refnr == null) {
                continue;
            }
            String title = firstText(job, "stellenangebotsTitel", "titel", "hauptberuf", "beruf");
            String occupation = firstText(job, "hauptberuf", "beruf");

            // Location: v6 "stellenlokationen[].adresse", v4 "arbeitsort"
            JsonNode address = job.path("stellenlokationen").path(0).path("adresse");
            if (address.isMissingNode()) {
                address = job.path("arbeitsort");
            }
            if (address.isMissingNode() && job.path("arbeitsorte").isArray()) {
                address = job.path("arbeitsorte").path(0);
            }
            String city = text(address, "ort");
            int extraLocations = Math.max(0, job.path("stellenlokationen").size() - 1);
            String location = city == null ? null : (extraLocations > 0 ? city + " (+" + extraLocations + " more)" : city);
            String country = Countries.normalize(text(address, "land"));
            if (country == null) {
                country = Countries.GERMANY;
            }

            String lowerTitle = title == null ? "" : title.toLowerCase(Locale.ROOT);
            boolean fullyRemote = job.path("homeofficeprozent").asInt(0) >= 100
                    || lowerTitle.contains("remote") || lowerTitle.contains("100% home");

            Set<String> tags = new LinkedHashSet<>();
            job.path("alleBerufe").forEach(b -> tags.add(b.asText()));
            if (occupation != null) {
                tags.add(occupation);
            }
            if (job.path("homeofficemoeglich").asBoolean(false)) {
                tags.add("Home office possible");
            }
            tags.remove(title);

            result.add(new JobPosting(
                    SOURCE,
                    refnr,
                    title,
                    firstText(job, "firma", "arbeitgeber", "arbeitgeberName"),
                    location,
                    country,
                    fullyRemote ? Boolean.TRUE : null,
                    occupation,   // search results contain no description; it is loaded via the details call
                    JOB_PAGE + refnr,
                    JsonDates.parse(firstText(job, "datumErsteVeroeffentlichung", "aktuelleVeroeffentlichungsdatum",
                            "aenderungsdatum", "modifikationsTimestamp")),
                    new ArrayList<>(tags),
                    salary(job),
                    job.path("arbeitszeitVollzeit").asBoolean(false) ? "Full-time" : null));
        }
        return result;
    }

    /** Extracts the plain-text description from a /jobdetails response. */
    public static String parseDetailDescription(JsonNode root) {
        if (root == null) {
            return null;
        }
        String description = firstText(root, "stellenangebotsBeschreibung", "stellenbeschreibung", "beschreibung");
        if (description == null) {
            return null;
        }
        // The text often contains Markdown-style headings ("# Title"); make them plain
        String plain = TextUtils.stripHtml(description).replaceAll("(?m)^#{1,6}\\s*", "");
        return plain.strip();
    }

    /** The details endpoint expects the reference number Base64-encoded. */
    public static String encodeRefnr(String refnr) {
        return Base64.getEncoder().encodeToString(refnr.getBytes(StandardCharsets.UTF_8));
    }

    private static String salary(JsonNode job) {
        double from = job.path("gehaltsspanneVon").asDouble(0);
        double to = job.path("gehaltsspanneBis").asDouble(0);
        if (from <= 0 && to <= 0) {
            return null;
        }
        String range = from > 0 && to > 0 && from != to
                ? String.format(Locale.GERMANY, "€%,.0f – €%,.0f", from, to)
                : String.format(Locale.GERMANY, "€%,.0f", Math.max(from, to));
        String type = text(job, "artDerVerguetung");
        return type == null ? range : range + " (" + type.toLowerCase(Locale.ROOT).replace('_', ' ') + ")";
    }

    private static String firstText(JsonNode node, String... fields) {
        for (String field : fields) {
            String v = text(node, field);
            if (v != null) {
                return v;
            }
        }
        return null;
    }

    static String text(JsonNode node, String field) {
        if (node == null) {
            return null;
        }
        JsonNode v = node.get(field);
        if (v == null || v.isNull() || !v.isValueNode()) {
            return null;
        }
        String s = v.asText().strip();
        return s.isEmpty() ? null : s;
    }
}
