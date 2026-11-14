package ua.readmania.dto.catalog;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class UpdateBookRequest {

    @NotBlank(message = "Назва книги не може бути порожньою. Приклад: Кобзар")
    @Size(max = 255, message = "Назва книги не повинна перевищувати 255 символів")
    private String title;

    @NotBlank(message = "ISBN не може бути порожнім. Приклад: 978-966-03-7000-1")
    @Size(max = 20, message = "ISBN не повинен перевищувати 20 символів")
    private String isbn;

    @NotNull(message = "Ціна обов'язкова. Приклад: 299.00")
    @DecimalMin(value = "0.00", message = "Ціна не може бути від'ємною")
    private BigDecimal price;

    @Min(value = 0, message = "Кількість на складі не може бути від'ємною")
    private int stock = 0;

    @Size(max = 255, message = "Видавництво не повинно перевищувати 255 символів")
    private String publisher;

    @Min(value = 1450, message = "Рік видання не може бути раніше 1450")
    @jakarta.validation.constraints.Max(value = 2100, message = "Рік видання не може бути пізніше 2100")
    private Integer publicationYear;

    private String description;

    private Long genreId;

    private String genre;

    private String author;

    private List<String> authors = new ArrayList<>();

    private List<Long> authorIds = new ArrayList<>();

    public UpdateBookRequest() {
    }

    public UpdateBookRequest(String title, String isbn, BigDecimal price, int stock, String description, Long genreId, List<String> authors) {
        this.title = title;
        this.isbn = isbn;
        this.price = price;
        this.stock = stock;
        this.description = description;
        this.genreId = genreId;
        this.authors = authors;
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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Long getGenreId() {
        return genreId;
    }

    public void setGenreId(Long genreId) {
        this.genreId = genreId;
    }

    public String getGenre() {
        return genre;
    }

    public void setGenre(String genre) {
        this.genre = genre;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public List<String> getAuthors() {
        return authors;
    }

    public void setAuthors(List<String> authors) {
        this.authors = authors != null ? authors : new ArrayList<>();
    }

    public List<Long> getAuthorIds() {
        return authorIds;
    }

    public void setAuthorIds(List<Long> authorIds) {
        this.authorIds = authorIds != null ? authorIds : new ArrayList<>();
    }
}
