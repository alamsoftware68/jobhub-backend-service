package com.jobsearch.util;

import java.text.Normalizer;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Turns country names in any common spelling ("Deutschland", "DE", "UK"…) into one English name,
 * and guesses the country from a location string ("Berlin", "London, United Kingdom").
 */
public final class Countries {

    public static final String GERMANY = "Germany";

    private static final Map<String, String> ALIASES = new HashMap<>();
    private static final Map<String, String> ADZUNA_CODES = new HashMap<>();
    private static final Map<String, String> CITY_COUNTRY = new HashMap<>();

    static {
        alias(GERMANY, "germany", "deutschland", "de", "deu", "bundesrepublik deutschland");
        alias("Austria", "austria", "osterreich", "oesterreich", "at", "aut");
        alias("Switzerland", "switzerland", "schweiz", "suisse", "svizzera", "ch", "che");
        alias("Netherlands", "netherlands", "the netherlands", "niederlande", "nederland", "holland", "nl", "nld");
        alias("United Kingdom", "united kingdom", "uk", "gb", "great britain", "england", "scotland", "wales",
                "vereinigtes konigreich", "grossbritannien");
        alias("France", "france", "frankreich", "fr");
        alias("Spain", "spain", "spanien", "espana", "es");
        alias("Italy", "italy", "italien", "italia", "it");
        alias("Poland", "poland", "polen", "polska", "pl");
        alias("Belgium", "belgium", "belgien", "belgique", "be");
        alias("Luxembourg", "luxembourg", "luxemburg", "lu");
        alias("Ireland", "ireland", "irland", "ie");
        alias("Denmark", "denmark", "danemark", "dk");
        alias("Sweden", "sweden", "schweden", "se");
        alias("Norway", "norway", "norwegen", "no");
        alias("Finland", "finland", "finnland", "fi");
        alias("Portugal", "portugal", "pt");
        alias("Czech Republic", "czech republic", "czechia", "tschechien", "cz");
        alias("United States", "united states", "usa", "us", "united states of america", "vereinigte staaten");
        alias("Canada", "canada", "kanada", "ca");
        alias("India", "india", "indien", "in");

        ADZUNA_CODES.put(GERMANY, "de");
        ADZUNA_CODES.put("Austria", "at");
        ADZUNA_CODES.put("Switzerland", "ch");
        ADZUNA_CODES.put("Netherlands", "nl");
        ADZUNA_CODES.put("United Kingdom", "gb");
        ADZUNA_CODES.put("France", "fr");
        ADZUNA_CODES.put("Spain", "es");
        ADZUNA_CODES.put("Italy", "it");
        ADZUNA_CODES.put("Poland", "pl");
        ADZUNA_CODES.put("Belgium", "be");
        ADZUNA_CODES.put("United States", "us");
        ADZUNA_CODES.put("Canada", "ca");
        ADZUNA_CODES.put("India", "in");

        cities(GERMANY, "berlin", "hamburg", "munchen", "muenchen", "munich", "koln", "koeln", "cologne",
                "frankfurt", "frankfurt am main", "stuttgart", "dusseldorf", "duesseldorf", "dortmund", "essen",
                "leipzig", "bremen", "dresden", "hannover", "hanover", "nurnberg", "nuernberg", "nuremberg",
                "duisburg", "bochum", "wuppertal", "bielefeld", "bonn", "munster", "muenster", "karlsruhe",
                "mannheim", "augsburg", "wiesbaden", "aachen", "kiel", "freiburg", "freiburg im breisgau",
                "heidelberg", "darmstadt", "potsdam", "regensburg", "ingolstadt", "wurzburg", "wuerzburg",
                "ulm", "erlangen", "jena", "rostock", "mainz", "magdeburg", "halle", "kassel", "braunschweig",
                "chemnitz", "lubeck", "luebeck", "osnabruck", "osnabrueck", "oldenburg", "paderborn",
                "gottingen", "goettingen", "saarbrucken", "saarbruecken", "erfurt", "eschborn", "walldorf",
                "konstanz", "unterfohring", "unterfoehring", "garching", "ismaning", "sindelfingen", "boblingen",
                "boeblingen", "wolfsburg", "gutersloh", "guetersloh", "remote germany");
        cities("Austria", "wien", "vienna", "graz", "linz", "salzburg", "innsbruck");
        cities("Switzerland", "zurich", "zuerich", "geneva", "genf", "basel", "bern", "lausanne", "zug");
        cities("Netherlands", "amsterdam", "rotterdam", "utrecht", "eindhoven", "the hague", "den haag",
                "breda", "delft", "groningen", "leiden", "haarlem", "amersfoort");
        cities("United Kingdom", "london", "manchester", "edinburgh", "birmingham", "bristol", "cambridge",
                "oxford", "leeds", "glasgow");
        cities("France", "paris", "lyon", "toulouse", "marseille", "nice", "lille");
        cities("Spain", "madrid", "barcelona", "valencia", "malaga", "sevilla");
        cities("Poland", "warsaw", "warschau", "krakow", "wroclaw", "gdansk", "poznan");
        cities("Ireland", "dublin", "cork");
        cities("Belgium", "brussels", "brussel", "bruxelles", "antwerp", "ghent");
        cities("Portugal", "lisbon", "lissabon", "porto");
        cities("Czech Republic", "prague", "prag", "brno");
        cities("Denmark", "copenhagen", "kopenhagen", "aarhus");
        cities("Sweden", "stockholm", "gothenburg", "malmo");
        cities("Luxembourg", "luxembourg city");
    }

    /** English and German names of the same city, so "Munich" also finds "München". */
    private static final String[][] CITY_VARIANTS = {
            {"munich", "münchen", "muenchen"}, {"cologne", "köln", "koeln"}, {"nuremberg", "nürnberg", "nuernberg"},
            {"hanover", "hannover"}, {"dusseldorf", "düsseldorf", "duesseldorf"}, {"vienna", "wien"},
            {"zurich", "zürich", "zuerich"}, {"geneva", "genf", "genève"}, {"brunswick", "braunschweig"},
            {"frankfurt", "frankfurt am main"}, {"the hague", "den haag"}, {"prague", "prag"},
            {"warsaw", "warschau"}, {"copenhagen", "kopenhagen"}, {"lisbon", "lissabon"}, {"brussels", "brüssel", "bruxelles"}};

    private Countries() {
    }

    /** German spelling of a city for German job sites ("Munich" -> "münchen"); other names unchanged. */
    public static String germanCityName(String city) {
        java.util.List<String> variants = cityVariants(city);
        return variants.size() > 1 && variants.get(0).equals(city.strip().toLowerCase(Locale.ROOT))
                ? variants.get(1) : city.strip();
    }

    /** All spellings to search for a city name typed by the user (always includes the input itself). */
    public static java.util.List<String> cityVariants(String city) {
        String c = city.strip().toLowerCase(Locale.ROOT);
        for (String[] group : CITY_VARIANTS) {
            for (String v : group) {
                if (v.equals(c)) {
                    return java.util.List.of(group);
                }
            }
        }
        return java.util.List.of(c);
    }

    private static void alias(String name, String... aliases) {
        for (String a : aliases) {
            ALIASES.put(a, name);
        }
    }

    private static void cities(String country, String... names) {
        for (String c : names) {
            CITY_COUNTRY.put(c, country);
        }
    }

    /** "DEUTSCHLAND" -> "Germany", "uk" -> "United Kingdom"; unknown values are returned title-cased. */
    public static String normalize(String country) {
        if (country == null || country.isBlank()) {
            return null;
        }
        String known = ALIASES.get(key(country));
        if (known != null) {
            return known;
        }
        String s = country.strip().toLowerCase(Locale.ROOT);
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    /** Known country for the alias, or null when it is not a known country name. */
    public static String knownCountry(String text) {
        return text == null ? null : ALIASES.get(key(text));
    }

    /** Best guess from a free-text location such as "Berlin", "Munich, Bavaria" or "London, United Kingdom". */
    public static String fromLocation(String location) {
        if (location == null || location.isBlank()) {
            return null;
        }
        String[] parts = location.split("[,/()\\-–]");
        // A country name anywhere (usually the last part)
        for (int i = parts.length - 1; i >= 0; i--) {
            String c = ALIASES.get(key(parts[i]));
            if (c != null && key(parts[i]).length() > 2) { // ignore 2-letter codes inside free text
                return c;
            }
        }
        // A known city
        for (String part : parts) {
            String c = CITY_COUNTRY.get(key(part));
            if (c != null) {
                return c;
            }
        }
        String whole = CITY_COUNTRY.get(key(location));
        return whole;
    }

    public static boolean isGermany(String country) {
        return country == null || country.isBlank() || GERMANY.equals(normalize(country));
    }

    /** Adzuna country code, or null if Adzuna doesn't cover the country. */
    public static String adzunaCode(String country) {
        return ADZUNA_CODES.get(country == null || country.isBlank() ? GERMANY : normalize(country));
    }

    private static String key(String s) {
        String n = Normalizer.normalize(s.strip(), Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return n.toLowerCase(Locale.ROOT).replace("ß", "ss").replaceAll("\\s+", " ");
    }
}
