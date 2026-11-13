package ua.readmania.domain.catalog;

import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.jpa.domain.Specification;

public final class BookSpecifications {

    private BookSpecifications() {
    }

    public static Specification<Book> hasSearchQuery(String query) {
        return (root, cq, cb) -> {
            if (query == null || query.isBlank()) {
                return cb.conjunction();
            }
            if (cq != null) {
                cq.distinct(true);
            }
            String pattern = "%" + query.trim().toLowerCase() + "%";
            Join<Book, Author> authorsJoin = root.join("authors", JoinType.LEFT);

            Predicate titleMatch = cb.like(cb.lower(root.get("title")), pattern);
            Predicate authorMatch = cb.like(cb.lower(authorsJoin.get("name")), pattern);

            return cb.or(titleMatch, authorMatch);
        };
    }

    public static Specification<Book> hasGenreId(Long genreId) {
        return (root, cq, cb) -> {
            if (genreId == null) {
                return cb.conjunction();
            }
            return cb.equal(root.get("genre").get("id"), genreId);
        };
    }

    public static Specification<Book> hasAuthorId(Long authorId) {
        return (root, cq, cb) -> {
            if (authorId == null) {
                return cb.conjunction();
            }
            if (cq != null) {
                cq.distinct(true);
            }
            Join<Book, Author> authorsJoin = root.join("authors", JoinType.INNER);
            return cb.equal(authorsJoin.get("id"), authorId);
        };
    }

    public static Specification<Book> priceGreaterThanOrEqual(BigDecimal minPrice) {
        return (root, cq, cb) -> {
            if (minPrice == null) {
                return cb.conjunction();
            }
            return cb.greaterThanOrEqualTo(root.get("price"), minPrice);
        };
    }

    public static Specification<Book> priceLessThanOrEqual(BigDecimal maxPrice) {
        return (root, cq, cb) -> {
            if (maxPrice == null) {
                return cb.conjunction();
            }
            return cb.lessThanOrEqualTo(root.get("price"), maxPrice);
        };
    }

    public static Specification<Book> inStockOnly(Boolean inStockOnly) {
        return (root, cq, cb) -> {
            if (inStockOnly == null || !inStockOnly) {
                return cb.conjunction();
            }
            return cb.greaterThan(root.get("stock"), 0);
        };
    }

    public static Specification<Book> filter(String query, Long genreId, Long authorId, BigDecimal minPrice, BigDecimal maxPrice, Boolean inStock) {
        return (root, cq, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (query != null && !query.isBlank()) {
                String pattern = "%" + query.trim().toLowerCase() + "%";
                Join<Book, Author> authorsJoin = root.join("authors", JoinType.LEFT);
                Predicate titleMatch = cb.like(cb.lower(root.get("title")), pattern);
                Predicate authorMatch = cb.like(cb.lower(authorsJoin.get("name")), pattern);
                predicates.add(cb.or(titleMatch, authorMatch));
                if (cq != null) {
                    cq.distinct(true);
                }
            }

            if (genreId != null) {
                predicates.add(cb.equal(root.get("genre").get("id"), genreId));
            }

            if (authorId != null) {
                Join<Book, Author> authorsJoin = root.join("authors", JoinType.INNER);
                predicates.add(cb.equal(authorsJoin.get("id"), authorId));
                if (cq != null) {
                    cq.distinct(true);
                }
            }

            if (minPrice != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("price"), minPrice));
            }

            if (maxPrice != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("price"), maxPrice));
            }

            if (inStock != null && inStock) {
                predicates.add(cb.greaterThan(root.get("stock"), 0));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    public static Specification<Book> filter(String query, String genreName, Long genreId, String authorName, Long authorId, BigDecimal minPrice, BigDecimal maxPrice, Boolean inStock) {
        return (root, cq, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (query != null && !query.isBlank()) {
                String pattern = "%" + query.trim().toLowerCase() + "%";
                Join<Book, Author> authorsJoin = root.join("authors", JoinType.LEFT);
                Predicate titleMatch = cb.like(cb.lower(root.get("title")), pattern);
                Predicate authorMatch = cb.like(cb.lower(authorsJoin.get("name")), pattern);
                predicates.add(cb.or(titleMatch, authorMatch));
                if (cq != null) {
                    cq.distinct(true);
                }
            }

            if (genreId != null) {
                predicates.add(cb.equal(root.get("genre").get("id"), genreId));
            } else if (genreName != null && !genreName.isBlank()) {
                Join<Book, Genre> genreJoin = root.join("genre", JoinType.INNER);
                predicates.add(cb.equal(cb.lower(genreJoin.get("name")), genreName.trim().toLowerCase()));
            }

            if (authorId != null) {
                Join<Book, Author> authorsJoin = root.join("authors", JoinType.INNER);
                predicates.add(cb.equal(authorsJoin.get("id"), authorId));
                if (cq != null) {
                    cq.distinct(true);
                }
            } else if (authorName != null && !authorName.isBlank()) {
                Join<Book, Author> authorsJoin = root.join("authors", JoinType.INNER);
                predicates.add(cb.like(cb.lower(authorsJoin.get("name")), "%" + authorName.trim().toLowerCase() + "%"));
                if (cq != null) {
                    cq.distinct(true);
                }
            }

            if (minPrice != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("price"), minPrice));
            }

            if (maxPrice != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("price"), maxPrice));
            }

            if (inStock != null && inStock) {
                predicates.add(cb.greaterThan(root.get("stock"), 0));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
