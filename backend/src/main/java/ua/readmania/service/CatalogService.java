package ua.readmania.service;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ua.readmania.common.NotFoundException;
import ua.readmania.domain.catalog.Author;
import ua.readmania.domain.catalog.AuthorRepository;
import ua.readmania.domain.catalog.Book;
import ua.readmania.domain.catalog.BookCover;
import ua.readmania.domain.catalog.BookCoverRepository;
import ua.readmania.domain.catalog.BookRepository;
import ua.readmania.domain.catalog.BookSpecifications;
import ua.readmania.domain.catalog.Genre;
import ua.readmania.domain.catalog.GenreRepository;
import ua.readmania.domain.loyalty.RatingRepository;
import ua.readmania.dto.catalog.AuthorResponse;
import ua.readmania.dto.catalog.BookDetailsResponse;
import ua.readmania.dto.catalog.BookSummaryResponse;
import ua.readmania.dto.catalog.GenreResponse;

@Service
@Transactional(readOnly = true)
public class CatalogService {

    private final BookRepository bookRepository;
    private final BookCoverRepository bookCoverRepository;
    private final GenreRepository genreRepository;
    private final AuthorRepository authorRepository;
    private final RatingRepository ratingRepository;

    public CatalogService(BookRepository bookRepository,
                          BookCoverRepository bookCoverRepository,
                          GenreRepository genreRepository,
                          AuthorRepository authorRepository,
                          RatingRepository ratingRepository) {
        this.bookRepository = bookRepository;
        this.bookCoverRepository = bookCoverRepository;
        this.genreRepository = genreRepository;
        this.authorRepository = authorRepository;
        this.ratingRepository = ratingRepository;
    }

    public Page<BookSummaryResponse> getBooks(String query,
                                             String genre,
                                             Long genreId,
                                             String author,
                                             Long authorId,
                                             BigDecimal priceMin,
                                             BigDecimal priceMax,
                                             Boolean inStock,
                                             Pageable pageable) {
        Specification<Book> spec = BookSpecifications.filter(
                query, genre, genreId, author, authorId, priceMin, priceMax, inStock
        );

        Page<Book> bookPage = bookRepository.findAll(spec, pageable);
        List<Book> books = bookPage.getContent();
        if (books.isEmpty()) {
            return bookPage.map(b -> null);
        }

        List<Long> bookIds = books.stream().map(Book::getId).toList();

        // Batch aggregate ratings
        Map<Long, RatingAggregation> ratingMap = new HashMap<>();
        List<Object[]> ratingStats = ratingRepository.findAverageRatingsByBookIds(bookIds);
        for (Object[] row : ratingStats) {
            Long bId = (Long) row[0];
            Double avg = (Double) row[1];
            Long cnt = (Long) row[2];
            ratingMap.put(bId, new RatingAggregation(avg, cnt != null ? cnt.intValue() : 0));
        }

        // Batch check cover existence
        Set<Long> coversPresent = new HashSet<>(bookCoverRepository.findAllById(bookIds).stream().map(BookCover::getBookId).toList());

        return bookPage.map(book -> {
            RatingAggregation agg = ratingMap.get(book.getId());
            Double avgRating = agg != null ? agg.average() : null;
            Integer ratingsCount = agg != null ? agg.count() : 0;
            boolean hasCover = coversPresent.contains(book.getId());
            return BookSummaryResponse.fromEntity(book, avgRating, ratingsCount, hasCover);
        });
    }

    public BookDetailsResponse getBookById(Long id) {
        Book book = bookRepository.findById(id)
                .filter(b -> !b.isDeleted())
                .orElseThrow(() -> new NotFoundException("Книгу з id " + id + " не знайдено"));

        Double averageRating = ratingRepository.findAverageRatingByBookId(id);
        Long countLong = ratingRepository.countByBookId(id);
        int ratingsCount = countLong != null ? countLong.intValue() : 0;
        boolean hasCover = bookCoverRepository.existsById(id);

        return BookDetailsResponse.fromEntity(book, averageRating, ratingsCount, hasCover);
    }

    public BookCover getBookCover(Long bookId) {
        Book book = bookRepository.findById(bookId)
                .filter(b -> !b.isDeleted())
                .orElseThrow(() -> new NotFoundException("Книгу з id " + bookId + " не знайдено"));

        return bookCoverRepository.findById(bookId)
                .orElseThrow(() -> new NotFoundException("Обкладинку для книги з id " + bookId + " не знайдено"));
    }

    public List<GenreResponse> getGenres() {
        return genreRepository.findAll(Sort.by("name")).stream()
                .map(GenreResponse::fromEntity)
                .toList();
    }

    public List<AuthorResponse> getAuthors() {
        return authorRepository.findAll(Sort.by("name")).stream()
                .map(AuthorResponse::fromEntity)
                .toList();
    }

    private record RatingAggregation(Double average, int count) {}
}
