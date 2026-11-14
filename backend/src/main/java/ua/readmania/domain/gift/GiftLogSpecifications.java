package ua.readmania.domain.gift;

import jakarta.persistence.criteria.Predicate;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.jpa.domain.Specification;

public final class GiftLogSpecifications {

    private GiftLogSpecifications() {
    }

    public static Specification<GiftLog> withFilters(Long institutionId, String institutionName, LocalDate dateFrom, LocalDate dateTo) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (institutionId != null) {
                predicates.add(cb.equal(root.get("institution").get("id"), institutionId));
            }

            if (institutionName != null && !institutionName.isBlank()) {
                predicates.add(cb.like(cb.lower(root.get("institutionName")), "%" + institutionName.trim().toLowerCase() + "%"));
            }

            if (dateFrom != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("deliveredDate"), dateFrom));
            }

            if (dateTo != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("deliveredDate"), dateTo));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
