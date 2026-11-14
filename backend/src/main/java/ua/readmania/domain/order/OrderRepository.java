package ua.readmania.domain.order;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long>, JpaSpecificationExecutor<Order> {

    @EntityGraph(attributePaths = {"user", "institution", "items", "items.book"})
    Optional<Order> findWithDetailsById(Long id);

    @EntityGraph(attributePaths = {"institution", "items"})
    Page<Order> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    @EntityGraph(attributePaths = {"user", "institution", "items"})
    Page<Order> findAllByOrderByCreatedAtDesc(Pageable pageable);

    @EntityGraph(attributePaths = {"user", "institution", "items"})
    Page<Order> findByStatusOrderByCreatedAtDesc(OrderStatus status, Pageable pageable);

    @Query("SELECT o FROM Order o JOIN o.items oi WHERE o.user.id = :userId AND o.status = ua.readmania.domain.order.OrderStatus.DELIVERED AND oi.book.id = :bookId ORDER BY COALESCE(o.deliveredAt, o.statusChangedAt, o.createdAt) DESC")
    List<Order> findDeliveredOrdersByUserIdAndBookId(@Param("userId") Long userId, @Param("bookId") Long bookId);

    @Query("SELECT o FROM Order o WHERE o.status = 'AWAITING_PAYMENT' AND o.createdAt < :before")
    List<Order> findUnpaidOrdersOlderThan(@Param("before") Instant before);

    @Query("SELECT o FROM Order o WHERE o.status = 'AWAITING_PAYMENT' AND o.createdAt < :before ORDER BY o.createdAt DESC")
    Page<Order> findUnpaidOrdersOlderThanPaged(@Param("before") Instant before, Pageable pageable);

    @Query("SELECT o FROM Order o WHERE o.status IN ('DELIVERED', 'CANCELLED') AND COALESCE(o.statusChangedAt, o.createdAt) < :before")
    List<Order> findArchivedOrdersOlderThan(@Param("before") Instant before);

    @Query("SELECT o FROM Order o WHERE o.status IN ('DELIVERED', 'CANCELLED') AND COALESCE(o.statusChangedAt, o.createdAt) < :before ORDER BY o.createdAt DESC")
    Page<Order> findArchivedOrdersOlderThanPaged(@Param("before") Instant before, Pageable pageable);

    @Query("SELECT o.status, COUNT(o) FROM Order o WHERE o.user.id = :userId GROUP BY o.status")
    List<Object[]> countOrdersByStatusForUser(@Param("userId") Long userId);

    @org.springframework.data.jpa.repository.Modifying
    @Query("UPDATE Order o SET o.user = null, o.recipientName = 'Видалений користувач', o.recipientPhone = null, o.deliveryAddress = null WHERE o.user.id IN :userIds")
    int anonymizeOrdersForUsers(@Param("userIds") Collection<Long> userIds);

    boolean existsByInstitutionIdAndStatusIn(Long institutionId, Collection<OrderStatus> statuses);

    boolean existsByInstitutionIdInAndStatusIn(Collection<Long> institutionIds, Collection<OrderStatus> statuses);

    List<Order> findByInstitutionIdInAndStatusIn(Collection<Long> institutionIds, Collection<OrderStatus> statuses);

    long countByInstitutionId(Long institutionId);

    @org.springframework.data.jpa.repository.Modifying
    @Query("DELETE FROM Order o WHERE o.institution.id IN :institutionIds AND o.status IN :statuses")
    int deleteByInstitutionIdInAndStatusIn(@Param("institutionIds") Collection<Long> institutionIds, @Param("statuses") Collection<OrderStatus> statuses);
}
