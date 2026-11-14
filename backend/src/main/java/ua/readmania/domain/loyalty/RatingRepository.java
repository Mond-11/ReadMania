package ua.readmania.domain.loyalty;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface RatingRepository extends JpaRepository<Rating, Long> {

    List<Rating> findByBookId(Long bookId);

    List<Rating> findByBookIdOrderByCreatedAtDesc(Long bookId);

    List<Rating> findByUserIdOrderByCreatedAtDesc(Long userId);

    Optional<Rating> findByUserIdAndBookId(Long userId, Long bookId);

    List<Rating> findByUserIdAndBookIdIn(Long userId, Collection<Long> bookIds);

    boolean existsByUserIdAndBookId(Long userId, Long bookId);

    @Query("SELECT AVG(r.value) FROM Rating r WHERE r.book.id = :bookId")
    Double findAverageRatingByBookId(@Param("bookId") Long bookId);

    @Query("SELECT COUNT(r) FROM Rating r WHERE r.book.id = :bookId")
    Long countByBookId(@Param("bookId") Long bookId);

    @Query("SELECT r.book.id, AVG(r.value), COUNT(r) FROM Rating r WHERE r.book.id IN :bookIds GROUP BY r.book.id")
    List<Object[]> findAverageRatingsByBookIds(@Param("bookIds") Collection<Long> bookIds);
}
