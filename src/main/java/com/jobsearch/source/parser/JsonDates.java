package com.jobsearch.source.parser;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

/** Lenient date parsing — each API uses a different format. Returns null when unparseable. */
final class JsonDates {

    private JsonDates() {
    }

    static Instant parse(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String v = value.strip();
        try {
            return OffsetDateTime.parse(v).toInstant();
        } catch (Exception ignored) {
            // try next format
        }
        try {
            return Instant.parse(v);
        } catch (Exception ignored) {
            // try next format
        }
        try {
            String local = v.length() > 19 ? v.substring(0, 19) : v;
            return LocalDateTime.parse(local).toInstant(ZoneOffset.UTC);
        } catch (Exception ignored) {
            // try next format
        }
        try {
            return LocalDate.parse(v.length() > 10 ? v.substring(0, 10) : v).atStartOfDay().toInstant(ZoneOffset.UTC);
        } catch (Exception ignored) {
            return null;
        }
    }

    static Instant fromEpochSeconds(long seconds) {
        return seconds > 0 ? Instant.ofEpochSecond(seconds) : null;
    }
}
