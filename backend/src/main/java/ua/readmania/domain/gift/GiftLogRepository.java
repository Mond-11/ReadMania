package ua.readmania.domain.gift;

import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface GiftLogRepository extends JpaRepository<GiftLog, Long> {

    @EntityGraph(attributePaths = {"institution", "order"})
    List<GiftLog> findByInstitutionIdOrderByDeliveredDateDesc(Long institutionId);

    @EntityGraph(attributePaths = {"institution", "order"})
    Page<GiftLog> findAllByOrderByDeliveredDateDesc(Pageable pageable);

    List<GiftLog> findByOrderId(Long orderId);
}
