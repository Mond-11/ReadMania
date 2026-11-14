package ua.readmania.domain.gift;

import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface GiftLogRepository extends JpaRepository<GiftLog, Long>, JpaSpecificationExecutor<GiftLog> {

    @EntityGraph(attributePaths = {"institution", "order"})
    List<GiftLog> findByInstitutionIdOrderByDeliveredDateDesc(Long institutionId);

    @EntityGraph(attributePaths = {"institution", "order"})
    Page<GiftLog> findAllByOrderByDeliveredDateDesc(Pageable pageable);

    List<GiftLog> findByOrderId(Long orderId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE GiftLog g SET g.institution = NULL WHERE g.institution.id IN :institutionIds")
    void detachInstitutions(@Param("institutionIds") List<Long> institutionIds);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE GiftLog g SET g.order = NULL WHERE g.order.id IN :orderIds")
    void detachOrders(@Param("orderIds") List<Long> orderIds);
}
