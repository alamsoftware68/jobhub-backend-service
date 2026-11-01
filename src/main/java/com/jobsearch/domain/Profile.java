package com.jobsearch.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Single-user profile (id = 1). List fields are stored comma-separated. */
@Entity
@Table(name = "profile")
public class Profile {

    public static final long SINGLE_USER_ID = 1L;

    @Id
    private Long id;

    @Column(name = "full_name")
    private String fullName;

    @Column(nullable = false, columnDefinition = "text")
    private String skills = "";

    @Column(name = "search_keywords", nullable = false, columnDefinition = "text")
    private String searchKeywords = "";

    @Column(nullable = false, columnDefinition = "text")
    private String locations = "";

    @Column(name = "radius_km", nullable = false)
    private int radiusKm = 50;

    @Column(name = "remote_preference", nullable = false)
    private String remotePreference = "ANY";

    @Column(name = "english_only", nullable = false)
    private boolean englishOnly;

    @Column(name = "exclude_keywords", nullable = false, columnDefinition = "text")
    private String excludeKeywords = "";

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public String getSkills() { return skills; }
    public void setSkills(String skills) { this.skills = skills; }
    public String getSearchKeywords() { return searchKeywords; }
    public void setSearchKeywords(String searchKeywords) { this.searchKeywords = searchKeywords; }
    public String getLocations() { return locations; }
    public void setLocations(String locations) { this.locations = locations; }
    public int getRadiusKm() { return radiusKm; }
    public void setRadiusKm(int radiusKm) { this.radiusKm = radiusKm; }
    public String getRemotePreference() { return remotePreference; }
    public void setRemotePreference(String remotePreference) { this.remotePreference = remotePreference; }
    public boolean isEnglishOnly() { return englishOnly; }
    public void setEnglishOnly(boolean englishOnly) { this.englishOnly = englishOnly; }
    public String getExcludeKeywords() { return excludeKeywords; }
    public void setExcludeKeywords(String excludeKeywords) { this.excludeKeywords = excludeKeywords; }
}
