package com.jobsearch.service;

import com.jobsearch.domain.Profile;
import com.jobsearch.repository.ProfileRepository;
import com.jobsearch.util.TextUtils;
import com.jobsearch.web.dto.ProfileDto;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProfileService {

    private final ProfileRepository repository;

    public ProfileService(ProfileRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public Profile get() {
        return repository.findById(Profile.SINGLE_USER_ID).orElseGet(() -> {
            Profile p = new Profile();
            p.setId(Profile.SINGLE_USER_ID);
            p.setSearchKeywords("Java Developer");
            p.setLocations("Deutschland");
            return repository.save(p);
        });
    }

    @Transactional
    public Profile update(ProfileDto dto) {
        Profile p = get();
        p.setFullName(dto.fullName());
        p.setSkills(TextUtils.joinCsv(dto.skills()));
        p.setSearchKeywords(TextUtils.joinCsv(dto.searchKeywords()));
        p.setLocations(TextUtils.joinCsv(dto.locations()));
        p.setRadiusKm(dto.radiusKm());
        p.setRemotePreference(dto.remotePreference());
        p.setEnglishOnly(dto.englishOnly());
        p.setExcludeKeywords(TextUtils.joinCsv(dto.excludeKeywords()));
        return repository.save(p);
    }

    public MatchScorer.ProfileSnapshot snapshot() {
        Profile p = get();
        return new MatchScorer.ProfileSnapshot(
                TextUtils.splitCsv(p.getSkills()),
                TextUtils.splitCsv(p.getSearchKeywords()),
                TextUtils.splitCsv(p.getLocations()),
                p.getRemotePreference(),
                p.isEnglishOnly(),
                TextUtils.splitCsv(p.getExcludeKeywords()));
    }
}
