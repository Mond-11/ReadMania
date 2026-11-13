package ua.readmania.domain.order;

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
}
