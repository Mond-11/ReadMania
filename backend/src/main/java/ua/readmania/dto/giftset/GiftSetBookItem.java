package ua.readmania.dto.giftset;

import java.math.BigDecimal;
import java.util.List;
import ua.readmania.domain.catalog.Book;
import ua.readmania.dto.catalog.AuthorResponse;
import ua.readmania.dto.catalog.GenreResponse;

public class GiftSetBookItem {

    private Long id;
    private String title;
    private String isbn;
    private GenreResponse genre;
    private List<AuthorResponse> authors;
    private BigDecimal price;
    private int stock;
    private String coverUrl;

    public GiftSetBookItem() {
    }

    public static GiftSetBookItem fromEntity(Book book) {
        if (book == null) {
            return null;
        }
        GiftSetBookItem item = new GiftSetBookItem();
        item.setId(book.getId());
        item.setTitle(book.getTitle());
        item.setIsbn(book.getIsbn());
        if (book.getGenre() != null) {
            item.setGenre(GenreResponse.fromEntity(book.getGenre()));
        }
        if (book.getAuthors() != null) {
            item.setAuthors(book.getAuthors().stream()
                    .map(AuthorResponse::fromEntity)
                    .toList());
        }
        item.setPrice(book.getPrice());
        item.setStock(book.getStock());
        item.setCoverUrl("/api/catalog/books/" + book.getId() + "/cover");
        return item;
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
        this.authors = authors;
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

    public String getCoverUrl() {
        return coverUrl;
    }

    public void setCoverUrl(String coverUrl) {
        this.coverUrl = coverUrl;
    }
}
