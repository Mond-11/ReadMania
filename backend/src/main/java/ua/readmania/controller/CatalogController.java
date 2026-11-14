package ua.readmania.controller;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ua.readmania.common.BusinessRuleException;
import ua.readmania.domain.catalog.BookCover;
import ua.readmania.dto.catalog.AuthorResponse;
import ua.readmania.dto.catalog.BookDetailsResponse;
import ua.readmania.dto.catalog.BookSummaryResponse;
import ua.readmania.dto.catalog.GenreResponse;
import ua.readmania.service.CatalogService;

@RestController
@RequestMapping("/api/catalog")
public class CatalogController {

    private final CatalogService catalogService;

    public CatalogController(CatalogService catalogService) {
        this.catalogService = catalogService;
    }

    @GetMapping("/books")
    public Page<BookSummaryResponse> getBooks(
            @RequestParam(name = "query", required = false) String query,
            @RequestParam(name = "q", required = false) String q,
            @RequestParam(name = "genre", required = false) String genre,
            @RequestParam(name = "genreId", required = false) Long genreId,
            @RequestParam(name = "author", required = false) String author,
            @RequestParam(name = "authorId", required = false) Long authorId,
            @RequestParam(name = "priceMin", required = false) BigDecimal priceMin,
            @RequestParam(name = "priceMax", required = false) BigDecimal priceMax,
            @RequestParam(name = "stockMin", required = false) Integer stockMin,
            @RequestParam(name = "stockMax", required = false) Integer stockMax,
            @RequestParam(name = "inStock", required = false) Boolean inStock,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "10") int size,
            @RequestParam(name = "sort", required = false) String sort
    ) {
        if (page < 0) {
            throw new BusinessRuleException("Номер сторінки не може бути від'ємним");
        }
        if (size <= 0) {
            throw new BusinessRuleException("Розмір сторінки повинен бути більшим за 0");
        }
        if (priceMin != null && priceMin.compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessRuleException("Мінімальна ціна не може бути від'ємною");
        }
        if (priceMax != null && priceMax.compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessRuleException("Максимальна ціна не може бути від'ємною");
        }
        if (priceMin != null && priceMax != null && priceMin.compareTo(priceMax) > 0) {
            throw new BusinessRuleException("Мінімальна ціна не може бути більшою за максимальну ціну");
        }
        if (stockMin != null && stockMin < 0) {
            throw new BusinessRuleException("Мінімальна кількість не може бути від'ємною");
        }
        if (stockMax != null && stockMax < 0) {
            throw new BusinessRuleException("Максимальна кількість не може бути від'ємною");
        }
        if (stockMin != null && stockMax != null && stockMin > stockMax) {
            throw new BusinessRuleException("Мінімальна кількість не може бути більшою за максимальну кількість");
        }

        String searchQuery = (query != null && !query.isBlank()) ? query : q;
        Pageable pageable = createPageable(page, size, sort);

        return catalogService.getBooks(
                searchQuery, genre, genreId, author, authorId, priceMin, priceMax, stockMin, stockMax, inStock, pageable
        );
    }

    @GetMapping("/books/{id}")
    public BookDetailsResponse getBookById(@PathVariable("id") Long id) {
        return catalogService.getBookById(id);
    }

    @GetMapping("/books/{id}/cover")
    public ResponseEntity<byte[]> getBookCover(@PathVariable("id") Long id) {
        BookCover cover = catalogService.getBookCover(id);

        MediaType mediaType;
        try {
            mediaType = MediaType.parseMediaType(cover.getContentType());
        } catch (Exception e) {
            mediaType = MediaType.IMAGE_JPEG;
        }

        return ResponseEntity.ok()
                .contentType(mediaType)
                .contentLength(cover.getData().length)
                .cacheControl(CacheControl.maxAge(java.time.Duration.ofHours(24)).cachePublic())
                .body(cover.getData());
    }

    @GetMapping("/genres")
    public List<GenreResponse> getGenres() {
        return catalogService.getGenres();
    }

    @GetMapping("/authors")
    public List<AuthorResponse> getAuthors() {
        return catalogService.getAuthors();
    }

    private Pageable createPageable(int page, int size, String sortParam) {
        if (sortParam == null || sortParam.isBlank()) {
            return PageRequest.of(page, size, Sort.by("id").descending());
        }

        List<Sort.Order> orders = new ArrayList<>();
        String[] sortParts = sortParam.split(",");
        String property = sortParts[0].trim();
        Sort.Direction direction = Sort.Direction.ASC;
        if (sortParts.length > 1 && sortParts[1].trim().equalsIgnoreCase("desc")) {
            direction = Sort.Direction.DESC;
        }

        // Map allowed properties
        String mappedProperty = switch (property.toLowerCase()) {
            case "price" -> "price";
            case "title" -> "title";
            case "stock" -> "stock";
            case "publicationyear", "publication_year", "year" -> "publicationYear";
            case "createdat", "created_at" -> "createdAt";
            default -> "id";
        };

        orders.add(new Sort.Order(direction, mappedProperty));
        return PageRequest.of(page, size, Sort.by(orders));
    }
}
