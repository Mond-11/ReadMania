package ua.readmania.dto.rating;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public class CreateRatingRequest {

    @NotNull(message = "ID книги є обов'язковим")
    private Long bookId;

    private Long orderId;

    @NotNull(message = "Оцінка є обов'язковою")
    @Min(value = 1, message = "Оцінка повинна бути від 1 до 5")
    @Max(value = 5, message = "Оцінка повинна бути від 1 до 5")
    private Integer value;

    public CreateRatingRequest() {
    }

    public CreateRatingRequest(Long bookId, Long orderId, Integer value) {
        this.bookId = bookId;
        this.orderId = orderId;
        this.value = value;
    }

    public Long getBookId() {
        return bookId;
    }

    public void setBookId(Long bookId) {
        this.bookId = bookId;
    }

    public Long getOrderId() {
        return orderId;
    }

    public void setOrderId(Long orderId) {
        this.orderId = orderId;
    }

    public Integer getValue() {
        return value;
    }

    public void setValue(Integer value) {
        this.value = value;
    }
}
