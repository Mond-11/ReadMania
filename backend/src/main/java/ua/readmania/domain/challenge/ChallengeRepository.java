package ua.readmania.domain.challenge;

import java.time.Instant;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ChallengeRepository extends JpaRepository<Challenge, Long> {

    List<Challenge> findByStatus(ChallengeStatus status);

    List<Challenge> findByStatusOrderByDeadlineAsc(ChallengeStatus status);

    List<Challenge> findAllByOrderByCreatedAtDesc();

    List<Challenge> findByStatusOrderByCreatedAtDesc(ChallengeStatus status);

    List<Challenge> findByStatusAndDeadlineBefore(ChallengeStatus status, Instant moment);
}
