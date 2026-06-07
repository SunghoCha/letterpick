package com.sungho.letterpick.trending.publicissue;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PublicIssueCandidateRepository extends JpaRepository<PublicIssueCandidate, Long> {

    Optional<PublicIssueCandidate> findByIssueId(Long issueId);
}
