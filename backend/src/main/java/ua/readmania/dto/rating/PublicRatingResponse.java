package ua.readmania.dto.rating;

import java.time.Instant;
import ua.readmania.domain.loyalty.Rating;

public class PublicRatingResponse {

    private Long id;
    private Long bookId;
    private int value;
    private Instant createdAt;

    public PublicRatingResponse() {
    }

    public PublicRatingResponse(Long id, Long bookId, int value, Instant createdAt) {
        this.id = id;
        this.bookId = bookId;
        this.value = value;
        this.createdAt = createdAt;
    }

    public static PublicRatingResponse fromEntity(Rating rating) {
        if (rating == null) {
            return null;
        }
        PublicRatingResponse response = new PublicRatingResponse();
        response.setId(rating.getId());
        if (rating.getBook() != null) {
            response.setBookId(rating.getBook().getId());
        }
        response.setValue(rating.getValue());
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

    public int getValue() {
        return value;
    }

    public void setValue(int value) {
        this.value = value;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
