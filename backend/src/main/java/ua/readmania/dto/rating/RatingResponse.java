package ua.readmania.dto.rating;

import java.time.Instant;
import ua.readmania.domain.loyalty.Rating;

public class RatingResponse {

    private Long id;
    private Long bookId;
    private String bookTitle;
    private Long orderId;
    private int value;
    private Long userId;
    private String userFullName;
    private Instant createdAt;

    public RatingResponse() {
    }

    public static RatingResponse fromEntity(Rating rating) {
        if (rating == null) {
            return null;
        }
        RatingResponse response = new RatingResponse();
        response.setId(rating.getId());
        if (rating.getBook() != null) {
            response.setBookId(rating.getBook().getId());
            response.setBookTitle(rating.getBook().getTitle());
        }
        if (rating.getOrder() != null) {
            response.setOrderId(rating.getOrder().getId());
        }
        response.setValue(rating.getValue());
        if (rating.getUser() != null) {
            response.setUserId(rating.getUser().getId());
            response.setUserFullName(rating.getUser().getFullName());
        }
        response.setCreatedAt(rating.getCreatedAt());
        return response;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getBookId() {
        return bookId;
    }

    public void setBookId(Long bookId) {
        this.bookId = bookId;
    }

    public String getBookTitle() {
        return bookTitle;
    }

    public void setBookTitle(String bookTitle) {
        this.bookTitle = bookTitle;
    }

    public Long getOrderId() {
        return orderId;
    }

    public void setOrderId(Long orderId) {
        this.orderId = orderId;
    }

    public int getValue() {
        return value;
    }

    public void setValue(int value) {
        this.value = value;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getUserFullName() {
        return userFullName;
    }

    public void setUserFullName(String userFullName) {
        this.userFullName = userFullName;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
