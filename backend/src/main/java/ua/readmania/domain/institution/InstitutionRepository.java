package ua.readmania.domain.institution;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface InstitutionRepository extends JpaRepository<Institution, Long> {

    List<Institution> findByType(InstitutionType type);

    List<Institution> findByNameContainingIgnoreCase(String name);

    boolean existsByNameIgnoreCase(String name);

    Optional<Institution> findByNameIgnoreCase(String name);

    @Query("SELECT i FROM Institution i WHERE " +
           "(:type IS NULL OR i.type = :type) AND " +
           "(:query IS NULL OR :query = '' OR LOWER(i.name) LIKE LOWER(CONCAT('%', :query, '%')) OR LOWER(i.address) LIKE LOWER(CONCAT('%', :query, '%'))) " +
           "ORDER BY i.name ASC")
    List<Institution> searchInstitutions(@Param("type") InstitutionType type, @Param("query") String query);
}
