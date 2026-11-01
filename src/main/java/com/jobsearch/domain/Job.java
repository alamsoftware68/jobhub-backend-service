package com.jobsearch.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "job")
public class Job {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String source;

    @Column(name = "external_id", nullable = false)
    private String externalId;

    @Column(nullable = false)
    private String title;

    private String company;
    private String location;
    private String country;
    private Boolean remote;

    @Column(columnDefinition = "text")
    private String description;

    @Column(nullable = false)
    private String url;

    @Column(name = "posted_at")
    private Instant postedAt;

    /** Calendar day (UTC) of postedAt, used for day-by-day sorting. */
    @Column(name = "posted_day")
    private LocalDate postedDay;

    @Column(name = "fetched_at", nullable = false)
    private Instant fetchedAt;

    /** Comma-separated tags. */
    @Column(columnDefinition = "text")
    private String tags;

    private String salary;

    @Column(name = "employment_type")
    private String employmentType;

    /** "de", "en" or "unknown". */
    private String language;

    @Column(name = "german_required", nullable = false)
    private boolean germanRequired;

    @Column(name = "dedup_key", nullable = false)
    private String dedupKey;

    /** The same job found on other sources, one "SOURCE|url" per line. */
    @Column(name = "other_sources", columnDefinition = "text")
    private String otherSources;

    @Column(name = "match_score", nullable = false)
    private int matchScore;

    @Column(name = "matched_skills", columnDefinition = "text")
    private String matchedSkills;

    @Column(name = "missing_skills", columnDefinition = "text")
    private String missingSkills;

    @Column(nullable = false)
    private boolean saved;

    @Column(nullable = false)
    private boolean hidden;

    @Column(name = "details_fetched", nullable = false)
    private boolean detailsFetched;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }
    public String getExternalId() { return externalId; }
    public void setExternalId(String externalId) { this.externalId = externalId; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getCompany() { return company; }
    public void setCompany(String company) { this.company = company; }
    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }
    public String getCountry() { return country; }
    public void setCountry(String country) { this.country = country; }
    public Boolean getRemote() { return remote; }
    public void setRemote(Boolean remote) { this.remote = remote; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }
    public Instant getPostedAt() { return postedAt; }
    public void setPostedAt(Instant postedAt) { this.postedAt = postedAt; }
    public LocalDate getPostedDay() { return postedDay; }
    public void setPostedDay(LocalDate postedDay) { this.postedDay = postedDay; }
    public Instant getFetchedAt() { return fetchedAt; }
    public void setFetchedAt(Instant fetchedAt) { this.fetchedAt = fetchedAt; }
    public String getTags() { return tags; }
    public void setTags(String tags) { this.tags = tags; }
    public String getSalary() { return salary; }
    public void setSalary(String salary) { this.salary = salary; }
    public String getEmploymentType() { return employmentType; }
    public void setEmploymentType(String employmentType) { this.employmentType = employmentType; }
    public String getLanguage() { return language; }
    public void setLanguage(String language) { this.language = language; }
    public boolean isGermanRequired() { return germanRequired; }
    public void setGermanRequired(boolean germanRequired) { this.germanRequired = germanRequired; }
    public String getDedupKey() { return dedupKey; }
    public void setDedupKey(String dedupKey) { this.dedupKey = dedupKey; }
    public String getOtherSources() { return otherSources; }
    public void setOtherSources(String otherSources) { this.otherSources = otherSources; }
    public int getMatchScore() { return matchScore; }
    public void setMatchScore(int matchScore) { this.matchScore = matchScore; }
    public String getMatchedSkills() { return matchedSkills; }
    public void setMatchedSkills(String matchedSkills) { this.matchedSkills = matchedSkills; }
    public String getMissingSkills() { return missingSkills; }
    public void setMissingSkills(String missingSkills) { this.missingSkills = missingSkills; }
    public boolean isSaved() { return saved; }
    public void setSaved(boolean saved) { this.saved = saved; }
    public boolean isHidden() { return hidden; }
    public void setHidden(boolean hidden) { this.hidden = hidden; }
    public boolean isDetailsFetched() { return detailsFetched; }
    public void setDetailsFetched(boolean detailsFetched) { this.detailsFetched = detailsFetched; }
}
