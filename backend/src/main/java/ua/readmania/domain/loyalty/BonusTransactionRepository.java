package ua.readmania.domain.loyalty;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BonusTransactionRepository extends JpaRepository<BonusTransaction, Long> {

    Page<BonusTransaction> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    List<BonusTransaction> findByUserIdOrderByCreatedAtDesc(Long userId);

    Optional<BonusTransaction> findByOrderIdAndType(Long orderId, BonusTransactionType type);

    Optional<BonusTransaction> findByUserIdAndChallengeIdAndType(Long userId, Long challengeId, BonusTransactionType type);

    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.data.jpa.repository.Query("DELETE FROM BonusTransaction bt WHERE bt.user.id = :userId")
    void deleteByUserId(@org.springframework.data.repository.query.Param("userId") Long userId);
}
