package ua.readmania.service;

import java.io.InputStream;
import java.time.Instant;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ua.readmania.common.BusinessRuleException;
import ua.readmania.common.ConflictException;
import ua.readmania.common.NotFoundException;
import ua.readmania.domain.catalog.Author;
import ua.readmania.domain.catalog.AuthorRepository;
import ua.readmania.domain.catalog.Book;
import ua.readmania.domain.catalog.BookCover;
import ua.readmania.domain.catalog.BookCoverRepository;
import ua.readmania.domain.catalog.BookRepository;
import ua.readmania.domain.catalog.Genre;
import ua.readmania.domain.catalog.GenreRepository;
import ua.readmania.domain.loyalty.RatingRepository;
import ua.readmania.domain.order.OrderItemRepository;
import ua.readmania.domain.order.OrderStatus;
import ua.readmania.dto.catalog.AuthorResponse;
import ua.readmania.dto.catalog.BookDetailsResponse;
import ua.readmania.dto.catalog.CreateAuthorRequest;
import ua.readmania.dto.catalog.CreateBookRequest;
import ua.readmania.dto.catalog.CsvImportResponse;
import ua.readmania.dto.catalog.GenreRequest;
import ua.readmania.dto.catalog.GenreResponse;
import ua.readmania.dto.catalog.UpdateBookRequest;

@Service
@Transactional
public class ManagerCatalogService {

    private final BookRepository bookRepository;
    private final GenreRepository genreRepository;
    private final AuthorRepository authorRepository;
    private final BookCoverRepository bookCoverRepository;
    private final OrderItemRepository orderItemRepository;
    private final RatingRepository ratingRepository;
    private final CsvImportService csvImportService;

    public ManagerCatalogService(BookRepository bookRepository,
                                 GenreRepository genreRepository,
                                 AuthorRepository authorRepository,
                                 BookCoverRepository bookCoverRepository,
                                 OrderItemRepository orderItemRepository,
                                 RatingRepository ratingRepository,
                                 CsvImportService csvImportService) {
        this.bookRepository = bookRepository;
        this.genreRepository = genreRepository;
        this.authorRepository = authorRepository;
        this.bookCoverRepository = bookCoverRepository;
        this.orderItemRepository = orderItemRepository;
        this.ratingRepository = ratingRepository;
        this.csvImportService = csvImportService;
    }

    public BookDetailsResponse createBook(CreateBookRequest request) {
        String cleanIsbn = request.getIsbn().trim();
        if (bookRepository.existsActiveByIsbn(cleanIsbn)) {
            throw new ConflictException("Книга з ISBN " + cleanIsbn + " вже існує");
        }

        Genre genre = resolveGenre(request.getGenreId(), request.getGenre());
        Set<Author> authors = resolveAuthors(request.getAuthorIds(), request.getAuthors(), request.getAuthor());

        Book book = new Book(
                request.getTitle().trim(),
                cleanIsbn,
                request.getPrice(),
                request.getStock(),
                genre,
                authors
        );
        if (request.getDescription() != null) {
            book.setDescription(request.getDescription().trim());
        }
        if (request.getPublisher() != null && !request.getPublisher().isBlank()) {
            book.setPublisher(request.getPublisher().trim());
        }
        if (request.getPublicationYear() != null) {
            book.setPublicationYear(request.getPublicationYear());
        }

        book = bookRepository.save(book);

        boolean canDelete = book.getStock() == 0 && !orderItemRepository.existsByBookIdAndOrderStatusIn(
                book.getId(),
                List.of(OrderStatus.AWAITING_PAYMENT, OrderStatus.PAID)
        );

        return BookDetailsResponse.fromEntity(book, null, 0, false, canDelete);
    }

    public BookDetailsResponse updateBook(Long id, UpdateBookRequest request) {
        Book book = bookRepository.findById(id)
                .filter(b -> !b.isDeleted())
                .orElseThrow(() -> new NotFoundException("Книгу з id " + id + " не знайдено"));

        String cleanIsbn = request.getIsbn().trim();
        if (bookRepository.existsActiveByIsbnAndIdNot(cleanIsbn, id)) {
            throw new ConflictException("Книга з ISBN " + cleanIsbn + " вже існує");
        }

        Genre genre = resolveGenre(request.getGenreId(), request.getGenre());
        Set<Author> authors = resolveAuthors(request.getAuthorIds(), request.getAuthors(), request.getAuthor());

        book.setTitle(request.getTitle().trim());
        book.setIsbn(cleanIsbn);
        book.setPrice(request.getPrice());
        book.setStock(request.getStock());
        book.setDescription(request.getDescription() != null ? request.getDescription().trim() : null);
        if (request.getPublisher() != null && !request.getPublisher().isBlank()) {
            book.setPublisher(request.getPublisher().trim());
        }
        if (request.getPublicationYear() != null) {
            book.setPublicationYear(request.getPublicationYear());
        }
        book.setGenre(genre);
        book.setAuthors(authors);

        book = bookRepository.save(book);

        Double avgRating = ratingRepository.findAverageRatingByBookId(id);
        Long countLong = ratingRepository.countByBookId(id);
        int ratingsCount = countLong != null ? countLong.intValue() : 0;
        boolean hasCover = bookCoverRepository.existsById(id);

        boolean canDelete = book.getStock() == 0 && !orderItemRepository.existsByBookIdAndOrderStatusIn(
                id,
                List.of(OrderStatus.AWAITING_PAYMENT, OrderStatus.PAID)
        );

        return BookDetailsResponse.fromEntity(book, avgRating, ratingsCount, hasCover, canDelete);
    }

    public void deleteBook(Long id) {
        Book book = bookRepository.findById(id)
                .filter(b -> !b.isDeleted())
                .orElseThrow(() -> new NotFoundException("Книгу з id " + id + " не знайдено"));

        if (book.getStock() > 0) {
            throw new BusinessRuleException("Неможливо видалити книгу з ненульовим залишком на складі (залишок: " + book.getStock() + ")");
        }

        boolean inActiveOrders = orderItemRepository.existsByBookIdAndOrderStatusIn(
                id,
                List.of(OrderStatus.AWAITING_PAYMENT, OrderStatus.PAID)
        );
        if (inActiveOrders) {
            throw new ConflictException("Неможливо видалити книгу, оскільки вона фігурує в активних замовленнях (зі статусом AWAITING_PAYMENT або PAID)");
        }

        book.setDeletedAt(Instant.now());
        bookRepository.save(book);
    }

    public GenreResponse createGenre(GenreRequest request) {
        String cleanName = request.getName().trim();
        if (genreRepository.existsByNameIgnoreCase(cleanName)) {
            throw new ConflictException("Жанр з назвою '" + cleanName + "' вже існує");
        }

        Genre genre = genreRepository.save(new Genre(cleanName));
        return GenreResponse.fromEntity(genre);
    }

    public GenreResponse updateGenre(Long id, GenreRequest request) {
        Genre genre = genreRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Жанр з id " + id + " не знайдено"));

        String cleanName = request.getName().trim();
        Optional<Genre> existing = genreRepository.findByNameIgnoreCase(cleanName);
        if (existing.isPresent() && !existing.get().getId().equals(id)) {
            throw new ConflictException("Жанр з назвою '" + cleanName + "' вже існує");
        }

        genre.setName(cleanName);
        genre = genreRepository.save(genre);
        return GenreResponse.fromEntity(genre);
    }

    public void deleteGenre(Long id) {
        Genre genre = genreRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Жанр з id " + id + " не знайдено"));

        if (bookRepository.existsAnyByGenreIdIncludingDeleted(id)) {
            throw new ConflictException("Неможливо видалити жанр, оскільки до нього прив'язані книги (у тому числі в архіві)");
        }

        genreRepository.delete(genre);
    }

    public AuthorResponse createAuthor(CreateAuthorRequest request) {
        String cleanName = request.getFullName().trim();
        if (authorRepository.existsByNameIgnoreCase(cleanName)) {
            throw new ConflictException("Автор з ім'ям '" + cleanName + "' вже існує");
        }

        Author author = authorRepository.save(new Author(cleanName));
        return AuthorResponse.fromEntity(author);
    }

    public void uploadCover(Long bookId, byte[] data, String contentType) {
        Book book = bookRepository.findById(bookId)
                .filter(b -> !b.isDeleted())
                .orElseThrow(() -> new NotFoundException("Книгу з id " + bookId + " не знайдено"));

        if (data == null || data.length == 0) {
            throw new BusinessRuleException("Файл обкладинки не може бути порожнім");
        }

        String mediaType = (contentType != null && !contentType.isBlank()) ? contentType : "image/jpeg";
        BookCover cover = bookCoverRepository.findById(bookId)
                .orElseGet(() -> new BookCover(book, mediaType, data));

        cover.setData(data);
        cover.setContentType(mediaType);
        bookCoverRepository.save(cover);
    }

    public CsvImportResponse importFromCsv(InputStream inputStream) {
        return csvImportService.importCsv(inputStream);
    }

    private Genre resolveGenre(Long genreId, String genreName) {
        if (genreId != null) {
            return genreRepository.findById(genreId)
                    .orElseThrow(() -> new NotFoundException("Жанр з id " + genreId + " не знайдено"));
        }
        if (genreName != null && !genreName.isBlank()) {
            String cleanName = genreName.trim();
            return genreRepository.findByNameIgnoreCase(cleanName)
                    .orElseGet(() -> genreRepository.save(new Genre(cleanName)));
        }
        throw new BusinessRuleException("Необхідно вказати жанр книги (genreId або genre)");
    }

    private Set<Author> resolveAuthors(List<Long> authorIds, List<String> authorNames, String authorSingle) {
        Set<Author> authors = new LinkedHashSet<>();

        if (authorIds != null && !authorIds.isEmpty()) {
            authors.addAll(authorRepository.findAllById(authorIds));
        }

        if (authorNames != null && !authorNames.isEmpty()) {
            for (String name : authorNames) {
                if (name != null && !name.isBlank()) {
                    String clean = name.trim();
                    Author author = authorRepository.findByNameIgnoreCase(clean)
                            .orElseGet(() -> authorRepository.save(new Author(clean)));
                    authors.add(author);
                }
            }
        }

        if (authorSingle != null && !authorSingle.isBlank()) {
            String[] parts = authorSingle.contains(";") ? authorSingle.split(";") : authorSingle.split(",");
            for (String part : parts) {
                String clean = part.trim();
                if (!clean.isBlank()) {
                    Author author = authorRepository.findByNameIgnoreCase(clean)
                            .orElseGet(() -> authorRepository.save(new Author(clean)));
                    authors.add(author);
                }
            }
        }

        if (authors.isEmpty()) {
            throw new BusinessRuleException("Необхідно вказати щонайменше одного автора для книги");
        }

        return authors;
    }
}
