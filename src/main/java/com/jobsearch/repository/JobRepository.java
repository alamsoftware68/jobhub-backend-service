package com.jobsearch.repository;

import com.jobsearch.domain.Job;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface JobRepository extends JpaRepository<Job, Long>, JpaSpecificationExecutor<Job> {

    Optional<Job> findBySourceAndExternalId(String source, String externalId);

    List<Job> findByDedupKey(String dedupKey);

    long countBySource(String source);

    long countBySaved(boolean saved);

    long countByFetchedAtAfter(Instant since);

    @Query("select distinct j.country from Job j where j.country is not null order by j.country")
    List<String> findDistinctCountries();

    @Modifying
    @Query("delete from Job j where j.saved = false and j.fetchedAt < :cutoff")
    int deleteUnsavedOlderThan(@Param("cutoff") Instant cutoff);
}
