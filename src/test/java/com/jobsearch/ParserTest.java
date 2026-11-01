package com.jobsearch;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jobsearch.source.JobPosting;
import com.jobsearch.source.parser.AdzunaParser;
import com.jobsearch.source.parser.ArbeitnowParser;
import com.jobsearch.source.parser.ArbeitsagenturParser;
import com.jobsearch.source.parser.JoobleParser;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ParserTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    static JsonNode fixture(String name) throws Exception {
        try (InputStream in = ParserTest.class.getResourceAsStream("/fixtures/" + name)) {
            return MAPPER.readTree(in);
        }
    }

    @Test
    void parsesArbeitsagenturSearch() throws Exception {
        List<JobPosting> jobs = ArbeitsagenturParser.parseSearch(fixture("arbeitsagentur-search.json"));
        assertEquals(2, jobs.size());
        JobPosting first = jobs.get(0);
        assertEquals("10000-1199999999-S", first.externalId());
        assertEquals("Beispiel Tech GmbH", first.company());
        assertEquals("Berlin", first.location());
        assertEquals("https://www.arbeitsagentur.de/jobsuche/jobdetail/10000-1199999999-S", first.url());
        assertNotNull(first.postedAt());
        // "Homeoffice möglich" means home office is possible, not a fully remote job
        assertEquals(null, jobs.get(1).remote());
        assertEquals("München", jobs.get(1).location());
        assertEquals("Germany", jobs.get(1).country());
    }

    @Test
    void parsesArbeitsagenturV6Search() throws Exception {
        List<JobPosting> jobs = ArbeitsagenturParser.parseSearch(fixture("arbeitsagentur-search-v6.json"));
        assertEquals(2, jobs.size());
        JobPosting first = jobs.get(0);
        assertEquals("13644-208756-S", first.externalId());
        assertEquals("Java Developer (w/m/d)", first.title());
        assertEquals("pulsation-it", first.company());
        assertEquals("Berlin", first.location());
        assertEquals("Germany", first.country());
        assertNotNull(first.postedAt());
        assertTrue(first.tags().contains("Home office possible"));

        JobPosting second = jobs.get(1);
        assertEquals("Berlin (+1 more)", second.location());
        assertEquals(Boolean.TRUE, second.remote());
        assertEquals("Full-time", second.employmentType());
        assertTrue(second.salary().contains("60.000"));
    }

    @Test
    void parsesArbeitsagenturDetailAndEncodesRefnr() throws Exception {
        String description = ArbeitsagenturParser.parseDetailDescription(fixture("arbeitsagentur-detail.json"));
        assertTrue(description.contains("Spring Boot"));
        assertEquals("MTAwMDEtMTAwMjcxNjkyMi1T", ArbeitsagenturParser.encodeRefnr("10001-1002716922-S"));
    }

    @Test
    void parsesArbeitnow() throws Exception {
        JsonNode root = fixture("arbeitnow.json");
        List<JobPosting> jobs = ArbeitnowParser.parse(root);
        assertEquals(2, jobs.size());
        JobPosting java = jobs.get(0);
        assertEquals(Boolean.TRUE, java.remote());
        assertFalse(java.description().contains("<p>"));
        assertTrue(java.description().contains("• REST APIs"));
        assertEquals("Germany", java.country());
        assertEquals("Full Time", java.employmentType());
        assertFalse(ArbeitnowParser.hasNextPage(root));
    }

    @Test
    void parsesAdzuna() throws Exception {
        List<JobPosting> jobs = AdzunaParser.parse(fixture("adzuna.json"));
        assertEquals(1, jobs.size());
        assertEquals("Java Backend Developer", jobs.get(0).title());
        assertEquals("Adzuna Kunde GmbH", jobs.get(0).company());
        assertNotNull(jobs.get(0).salary());
    }

    @Test
    void parsesJooble() throws Exception {
        List<JobPosting> jobs = JoobleParser.parse(fixture("jooble.json"));
        assertEquals(1, jobs.size());
        assertEquals("123456789", jobs.get(0).externalId());
        assertEquals(Boolean.TRUE, jobs.get(0).remote());
        assertTrue(jobs.get(0).tags().contains("via stepstone.de"));
    }
}
