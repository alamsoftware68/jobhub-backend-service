-- MySQL 8 schema for JobHub

CREATE TABLE job (
    id                BIGINT        NOT NULL AUTO_INCREMENT PRIMARY KEY,
    source            VARCHAR(50)   NOT NULL,
    external_id       VARCHAR(255)  NOT NULL,
    title             VARCHAR(500)  NOT NULL,
    company           VARCHAR(300),
    location          VARCHAR(300),
    remote            BOOLEAN,
    description       MEDIUMTEXT,
    url               VARCHAR(2000) NOT NULL,
    posted_at         DATETIME(6),
    fetched_at        DATETIME(6)   NOT NULL,
    tags              TEXT,
    salary            VARCHAR(200),
    employment_type   VARCHAR(200),
    language          VARCHAR(10),
    german_required   BOOLEAN       NOT NULL DEFAULT FALSE,
    dedup_key         VARCHAR(600)  NOT NULL,
    other_sources     TEXT,
    match_score       INT           NOT NULL DEFAULT 0,
    matched_skills    TEXT,
    missing_skills    TEXT,
    saved             BOOLEAN       NOT NULL DEFAULT FALSE,
    hidden            BOOLEAN       NOT NULL DEFAULT FALSE,
    details_fetched   BOOLEAN       NOT NULL DEFAULT FALSE,
    CONSTRAINT uq_job_source_external UNIQUE (source, external_id),
    INDEX idx_job_dedup_key (dedup_key),
    INDEX idx_job_match_score (match_score),
    INDEX idx_job_posted_at (posted_at)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE profile (
    id                BIGINT       NOT NULL PRIMARY KEY,
    full_name         VARCHAR(200),
    skills            TEXT         NOT NULL,
    search_keywords   TEXT         NOT NULL,
    locations         TEXT         NOT NULL,
    radius_km         INT          NOT NULL DEFAULT 50,
    remote_preference VARCHAR(20)  NOT NULL DEFAULT 'ANY',
    english_only      BOOLEAN      NOT NULL DEFAULT FALSE,
    exclude_keywords  TEXT         NOT NULL
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

-- Single-user profile, pre-filled from the CV. Edit it in the app's Profile page.
INSERT INTO profile (id, full_name, skills, search_keywords, locations, radius_km, remote_preference, english_only, exclude_keywords)
VALUES (
    1,
    'Mohammad Naushad Alam',
    'Java,Spring Boot,Microservices,REST,AWS,Spring Security,Spring MVC,Hibernate,JPA,React,JavaScript,Jenkins,CI/CD,Spring AI,LLM',
    'Java Developer,Java Backend,Spring Boot',
    'Berlin,Deutschland',
    50,
    'ANY',
    FALSE,
    'Praktikum,Werkstudent,Ausbildung,Internship'
);
