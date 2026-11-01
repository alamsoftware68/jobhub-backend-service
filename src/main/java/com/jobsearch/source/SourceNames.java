package com.jobsearch.source;

import com.jobsearch.source.parser.AdzunaParser;
import com.jobsearch.source.parser.ArbeitnowParser;
import com.jobsearch.source.parser.ArbeitsagenturParser;
import com.jobsearch.source.parser.JoobleParser;

import java.util.Map;

public final class SourceNames {

    private static final Map<String, String> NAMES = Map.of(
            ArbeitsagenturParser.SOURCE, "Arbeitsagentur",
            ArbeitnowParser.SOURCE, "Arbeitnow",
            AdzunaParser.SOURCE, "Adzuna",
            JoobleParser.SOURCE, "Jooble");

    private SourceNames() {
    }

    public static String displayName(String sourceId) {
        return NAMES.getOrDefault(sourceId, sourceId);
    }
}
