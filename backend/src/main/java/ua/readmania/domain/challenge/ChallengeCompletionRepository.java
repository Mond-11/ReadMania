package ua.readmania.domain.challenge;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ChallengeCompletionRepository extends JpaRepository<ChallengeCompletion, ChallengeCompletionId> {

    List<ChallengeCompletion> findByUserId(Long userId);

    boolean existsByUserIdAndChallengeId(Long userId, Long challengeId);
}
