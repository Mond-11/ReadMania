package ua.readmania.domain.order;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

    List<OrderItem> findByOrderId(Long orderId);

    @Query("SELECT COUNT(oi) > 0 FROM OrderItem oi WHERE oi.book.id = :bookId AND oi.order.status IN :statuses")
    boolean existsByBookIdAndOrderStatusIn(@Param("bookId") Long bookId, @Param("statuses") Collection<OrderStatus> statuses);

    @Query("SELECT COALESCE(SUM(oi.quantity), 0) FROM OrderItem oi JOIN oi.order o WHERE o.user.id = :userId AND o.status = ua.readmania.domain.order.OrderStatus.DELIVERED AND (cast(:fromDate as java.time.Instant) IS NULL OR COALESCE(o.deliveredAt, o.statusChangedAt, o.createdAt) >= :fromDate) AND (cast(:toDate as java.time.Instant) IS NULL OR COALESCE(o.deliveredAt, o.statusChangedAt, o.createdAt) <= :toDate) AND (:genreId IS NULL OR oi.book.genre.id = :genreId)")
    int countDeliveredBooksForChallenge(@Param("userId") Long userId, @Param("fromDate") Instant fromDate, @Param("toDate") Instant toDate, @Param("genreId") Long genreId);
}
