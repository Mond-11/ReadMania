package ua.readmania.domain.cart;

import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CartItemRepository extends JpaRepository<CartItem, CartItemId> {

    @EntityGraph(attributePaths = {"book", "book.genre", "book.authors"})
    List<CartItem> findByUserId(Long userId);

    void deleteByUserId(Long userId);

    void deleteByUserIdAndBookId(Long userId, Long bookId);
}
