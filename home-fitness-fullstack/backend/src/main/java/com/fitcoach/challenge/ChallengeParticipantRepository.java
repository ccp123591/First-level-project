package com.fitcoach.challenge;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ChallengeParticipantRepository extends JpaRepository<ChallengeParticipant, Long> {

    boolean existsByChallengeIdAndUserId(Long challengeId, Long userId);

    Optional<ChallengeParticipant> findByChallengeIdAndUserId(Long challengeId, Long userId);

    Page<ChallengeParticipant> findByChallengeIdOrderByProgressRepsDesc(Long challengeId, Pageable pageable);

    long countByChallengeId(Long challengeId);
}
