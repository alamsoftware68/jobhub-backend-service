package com.jobsearch.source;

import com.jobsearch.util.Countries;

import java.util.List;

/**
 * What to search for.
 * <ul>
 *   <li>{@code locations}: cities. "Deutschland"/"Germany"/blank means nationwide in {@code country}.</li>
 *   <li>{@code country}: English country name (e.g. "Germany", "Netherlands"). Null means Germany.</li>
 * </ul>
 */
public record SearchCriteria(
        List<String> keywords,
        List<String> locations,
        String country,
        int radiusKm,
        int maxDaysOld,
        int maxResultsPerSearch) {

    public SearchCriteria {
        country = country == null || country.isBlank() ? Countries.GERMANY : Countries.normalize(country);
    }

    public boolean isGermany() {
        return Countries.GERMANY.equals(country);
    }

    /** True for blank or a country name — i.e. "search the whole country", not a city. */
    public static boolean isNationwide(String location) {
        return location == null || location.isBlank() || Countries.knownCountry(location) != null;
    }
}
