package ua.readmania.domain.institution;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface InstitutionRepository extends JpaRepository<Institution, Long> {

    List<Institution> findByType(InstitutionType type);

    List<Institution> findByNameContainingIgnoreCase(String name);
}
