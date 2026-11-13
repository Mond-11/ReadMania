package ua.readmania.domain.catalog;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BookCoverRepository extends JpaRepository<BookCover, Long> {
}
