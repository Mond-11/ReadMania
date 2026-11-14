package ua.readmania.dto.catalog;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import ua.readmania.domain.catalog.Book;

public class BookDetailsResponse {

    private Long id;
    private String title;
    private String description;
    private String isbn;
    private GenreResponse genre;
    private List<AuthorResponse> authors = new ArrayList<>();
    private BigDecimal price;
    private int stock;
    private String publisher;
    private Integer publicationYear;
    private Long version;
    private Double averageRating;
    private Integer ratingsCount;
    private boolean hasCover;
    private String coverUrl;
    private Instant createdAt;
    private Instant updatedAt;

    public BookDetailsResponse() {
    }

    public static BookDetailsResponse fromEntity(Book book, Double averageRating, Integer ratingsCount, boolean hasCover) {
        if (book == null) {
            return null;
        }
        BookDetailsResponse response = new BookDetailsResponse();
        response.setId(book.getId());
        response.setTitle(book.getTitle());
        response.setDescription(book.getDescription());
        response.setIsbn(book.getIsbn());
        if (book.getGenre() != null) {
            response.setGenre(GenreResponse.fromEntity(book.getGenre()));
        }
        if (book.getAuthors() != null) {
            response.setAuthors(book.getAuthors().stream().map(AuthorResponse::fromEntity).toList());
        }
        response.setPrice(book.getPrice());
        response.setStock(book.getStock());
        response.setPublisher(book.getPublisher());
        response.setPublicationYear(book.getPublicationYear());
        response.setVersion(book.getVersion());
        response.setAverageRating(averageRating != null ? Math.round(averageRating * 10.0) / 10.0 : null);
        response.setRatingsCount(ratingsCount != null ? ratingsCount : 0);
        response.setHasCover(hasCover);
        if (hasCover) {
            response.setCoverUrl("/api/catalog/books/" + book.getId() + "/cover");
        }
        response.setCreatedAt(book.getCreatedAt());
        response.setUpdatedAt(book.getUpdatedAt());
        return response;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getIsbn() {
        return isbn;
    }

    public void setIsbn(String isbn) {
        this.isbn = isbn;
    }

    public GenreResponse getGenre() {
        return genre;
    }

    public void setGenre(GenreResponse genre) {
        this.genre = genre;
    }

    public List<AuthorResponse> getAuthors() {
        return authors;
    }

    public void setAuthors(List<AuthorResponse> authors) {
        this.authors = authors != null ? authors : new ArrayList<>();
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public int getStock() {
        return stock;
    }

    public void setStock(int stock) {
        this.stock = stock;
    }

    public String getPublisher() {
        return publisher;
    }

    public void setPublisher(String publisher) {
        this.publisher = publisher;
    }

    public Integer getPublicationYear() {
        return publicationYear;
    }

    public void setPublicationYear(Integer publicationYear) {
        this.publicationYear = publicationYear;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }

    public Double getAverageRating() {
        return averageRating;
    }

    public void setAverageRating(Double averageRating) {
        this.averageRating = averageRating;
    }

    public Integer getRatingsCount() {
        return ratingsCount;
    }

    public void setRatingsCount(Integer ratingsCount) {
        this.ratingsCount = ratingsCount;
    }

    public boolean isHasCover() {
        return hasCover;
    }

    public void setHasCover(boolean hasCover) {
        this.hasCover = hasCover;
    }

    public String getCoverUrl() {
        return coverUrl;
    }

    public void setCoverUrl(String coverUrl) {
        this.coverUrl = coverUrl;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
