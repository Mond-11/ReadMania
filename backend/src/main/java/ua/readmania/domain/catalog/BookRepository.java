package ua.readmania.domain.catalog;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface BookRepository extends JpaRepository<Book, Long>, JpaSpecificationExecutor<Book> {

    @Override
    @EntityGraph(attributePaths = {"genre", "authors"})
    Optional<Book> findById(Long id);

    @Override
    @EntityGraph(attributePaths = {"genre", "authors"})
    Page<Book> findAll(Pageable pageable);

    @Override
    @EntityGraph(attributePaths = {"genre", "authors"})
    Page<Book> findAll(Specification<Book> spec, Pageable pageable);

    @Override
    @EntityGraph(attributePaths = {"genre", "authors"})
    List<Book> findAll(Specification<Book> spec);

    @EntityGraph(attributePaths = {"genre", "authors"})
    Optional<Book> findByIsbn(String isbn);

    @EntityGraph(attributePaths = {"genre", "authors"})
    @Query("SELECT b FROM Book b WHERE b.genre.id IN :genreIds AND b.stock > 0")
    List<Book> findAvailableByGenreIds(@Param("genreIds") Collection<Long> genreIds);

    @EntityGraph(attributePaths = {"genre", "authors"})
    @Query("SELECT b FROM Book b WHERE b.genre.id IN :genreIds AND b.stock > 0 AND b.id NOT IN :excludedIds AND b.price <= :maxPrice")
    List<Book> findCandidatesForReplace(@Param("genreIds") Collection<Long> genreIds,
                                        @Param("excludedIds") Collection<Long> excludedIds,
                                        @Param("maxPrice") BigDecimal maxPrice);

    boolean existsByIsbn(String isbn);

    @Query(value = "SELECT COUNT(*) > 0 FROM books WHERE isbn = :isbn AND deleted_at IS NULL", nativeQuery = true)
    boolean existsActiveByIsbn(@Param("isbn") String isbn);

    @Query(value = "SELECT COUNT(*) > 0 FROM books WHERE isbn = :isbn AND id <> :id AND deleted_at IS NULL", nativeQuery = true)
    boolean existsActiveByIsbnAndIdNot(@Param("isbn") String isbn, @Param("id") Long id);

    @Query(value = "SELECT COUNT(*) > 0 FROM books WHERE genre_id = :genreId", nativeQuery = true)
    boolean existsAnyByGenreIdIncludingDeleted(@Param("genreId") Long genreId);

    @Modifying
    @Query(value = "UPDATE books SET deleted_at = CURRENT_TIMESTAMP WHERE id = :id", nativeQuery = true)
    int softDeleteById(@Param("id") Long id);

    @Query(value = "SELECT * FROM books WHERE id = :id", nativeQuery = true)
    Optional<Book> findByIdIncludingDeleted(@Param("id") Long id);
}
