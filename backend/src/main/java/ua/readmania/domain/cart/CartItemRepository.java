package ua.readmania.domain.cart;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface CartItemRepository extends JpaRepository<CartItem, CartItemId> {

    @EntityGraph(attributePaths = {"book", "book.genre", "book.authors"})
    List<CartItem> findByUserId(Long userId);

    @EntityGraph(attributePaths = {"book", "book.genre", "book.authors"})
    Optional<CartItem> findByUserIdAndBookId(Long userId, Long bookId);

    @Modifying
    @Query("DELETE FROM CartItem ci WHERE ci.id.userId = :userId")
    void deleteByUserId(@Param("userId") Long userId);

    @Modifying
    @Query("DELETE FROM CartItem ci WHERE ci.id.userId = :userId AND ci.id.bookId = :bookId")
    void deleteByUserIdAndBookId(@Param("userId") Long userId, @Param("bookId") Long bookId);
}
