package ua.readmania.domain.user;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UnblockRequestRepository extends JpaRepository<UnblockRequest, Long> {

    List<UnblockRequest> findByStatusOrderByCreatedAtDesc(UnblockRequestStatus status);

    List<UnblockRequest> findByUserIdOrderByCreatedAtDesc(Long userId);
}
