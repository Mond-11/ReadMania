package ua.readmania.domain.catalog;

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

    boolean existsByIsbn(String isbn);

    @Modifying
    @Query(value = "UPDATE books SET deleted_at = CURRENT_TIMESTAMP WHERE id = :id", nativeQuery = true)
    int softDeleteById(@Param("id") Long id);

    @Query(value = "SELECT * FROM books WHERE id = :id", nativeQuery = true)
    Optional<Book> findByIdIncludingDeleted(@Param("id") Long id);
}
