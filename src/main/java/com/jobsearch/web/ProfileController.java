package com.jobsearch.web;

import com.jobsearch.service.JobScoringService;
import com.jobsearch.service.ProfileService;
import com.jobsearch.web.dto.ProfileDto;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/profile")
public class ProfileController {

    private final ProfileService profiles;
    private final JobScoringService scoring;

    public ProfileController(ProfileService profiles, JobScoringService scoring) {
        this.profiles = profiles;
        this.scoring = scoring;
    }

    @GetMapping
    public ProfileDto get() {
        return ProfileDto.from(profiles.get());
    }

    /** Saves the profile and recalculates all match scores. */
    @PutMapping
    public ProfileDto update(@Valid @RequestBody ProfileDto dto) {
        ProfileDto saved = ProfileDto.from(profiles.update(dto));
        scoring.rescoreAll();
        return saved;
    }
}
