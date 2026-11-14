package ua.readmania.domain.user;

import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UnblockRequestRepository extends JpaRepository<UnblockRequest, Long> {

    @EntityGraph(attributePaths = {"user"})
    List<UnblockRequest> findAllByOrderByCreatedAtDesc();

    @EntityGraph(attributePaths = {"user"})
    List<UnblockRequest> findByStatusOrderByCreatedAtDesc(UnblockRequestStatus status);

    List<UnblockRequest> findByUserIdOrderByCreatedAtDesc(Long userId);

    java.util.Optional<UnblockRequest> findFirstByUserIdAndStatus(Long userId, UnblockRequestStatus status);

    java.util.Optional<UnblockRequest> findTopByUserIdOrderByCreatedAtDesc(Long userId);

    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.data.jpa.repository.Query("DELETE FROM UnblockRequest ur WHERE ur.user.id = :userId")
    void deleteByUserId(@org.springframework.data.repository.query.Param("userId") Long userId);
}
