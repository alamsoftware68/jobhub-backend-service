package com.jobsearch.web.dto;

import com.jobsearch.domain.Profile;
import com.jobsearch.util.TextUtils;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.util.List;

public record ProfileDto(
        String fullName,
        @NotNull List<String> skills,
        @NotEmpty List<String> searchKeywords,
        @NotNull List<String> locations,
        @Min(0) @Max(200) int radiusKm,
        @NotNull @Pattern(regexp = "ANY|REMOTE|HYBRID|ONSITE") String remotePreference,
        boolean englishOnly,
        @NotNull List<String> excludeKeywords) {

    public static ProfileDto from(Profile p) {
        return new ProfileDto(
                p.getFullName(),
                TextUtils.splitCsv(p.getSkills()),
                TextUtils.splitCsv(p.getSearchKeywords()),
                TextUtils.splitCsv(p.getLocations()),
                p.getRadiusKm(),
                p.getRemotePreference(),
                p.isEnglishOnly(),
                TextUtils.splitCsv(p.getExcludeKeywords()));
    }
}
