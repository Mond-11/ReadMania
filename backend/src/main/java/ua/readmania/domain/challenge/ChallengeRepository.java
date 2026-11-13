package ua.readmania.domain.challenge;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ChallengeRepository extends JpaRepository<Challenge, Long> {

    List<Challenge> findByStatus(ChallengeStatus status);

    List<Challenge> findByStatusOrderByDeadlineAsc(ChallengeStatus status);
}
