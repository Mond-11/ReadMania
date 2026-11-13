package ua.readmania.dto.cart;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public class AddToCartRequest {

    @NotNull(message = "Ідентифікатор книги є обов'язковим. Приклад: 1")
    private Long bookId;

    @Min(value = 1, message = "Кількість повинна бути щонайменше 1. Приклад: 1")
    private int quantity = 1;

    public AddToCartRequest() {
    }

    public AddToCartRequest(Long bookId, int quantity) {
        this.bookId = bookId;
        this.quantity = quantity;
    }

    public Long getBookId() {
        return bookId;
    }

    public void setBookId(Long bookId) {
        this.bookId = bookId;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }
}
