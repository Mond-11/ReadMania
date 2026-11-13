package ua.readmania.dto.cart;

import java.math.BigDecimal;
import ua.readmania.domain.cart.CartItem;
import ua.readmania.domain.catalog.Book;

public class CartItemResponse {

    private Long bookId;
    private String title;
    private String isbn;
    private BigDecimal price;
    private int quantity;
    private boolean available;
    private int stock;
    private BigDecimal subtotal;
    private boolean hasCover;
    private String coverUrl;

    public CartItemResponse() {
    }

    public static CartItemResponse fromEntity(CartItem item, boolean hasCover) {
        if (item == null) {
            return null;
        }
        Book book = item.getBook();
        CartItemResponse response = new CartItemResponse();
        if (book != null) {
            response.setBookId(book.getId());
            response.setTitle(book.getTitle());
            response.setIsbn(book.getIsbn());
            response.setPrice(book.getPrice());
            response.setStock(book.getStock());
            boolean available = (book.getDeletedAt() == null && book.getStock() > 0);
            response.setAvailable(available);
            if (book.getPrice() != null) {
                response.setSubtotal(book.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())));
            }
        }
        response.setQuantity(item.getQuantity());
        response.setHasCover(hasCover);
        if (hasCover && book != null) {
            response.setCoverUrl("/api/catalog/books/" + book.getId() + "/cover");
        }
        return response;
    }

    public Long getBookId() {
        return bookId;
    }

    public void setBookId(Long bookId) {
        this.bookId = bookId;
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

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public boolean isAvailable() {
        return available;
    }

    public void setAvailable(boolean available) {
        this.available = available;
    }

    public int getStock() {
        return stock;
    }

    public void setStock(int stock) {
        this.stock = stock;
    }

    public BigDecimal getSubtotal() {
        return subtotal;
    }

    public void setSubtotal(BigDecimal subtotal) {
        this.subtotal = subtotal;
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
}
