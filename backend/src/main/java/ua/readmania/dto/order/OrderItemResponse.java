package ua.readmania.dto.order;

import java.math.BigDecimal;
import ua.readmania.domain.order.OrderItem;

public class OrderItemResponse {

    private Long id;
    private Long bookId;
    private String titleSnapshot;
    private BigDecimal priceSnapshot;
    private int quantity;
    private BigDecimal subtotal;
    private Integer userRating;

    public OrderItemResponse() {
    }

    public static OrderItemResponse fromEntity(OrderItem item) {
        return fromEntity(item, null);
    }

    public static OrderItemResponse fromEntity(OrderItem item, Integer userRating) {
        if (item == null) {
            return null;
        }
        OrderItemResponse response = new OrderItemResponse();
        response.setId(item.getId());
        if (item.getBook() != null) {
            response.setBookId(item.getBook().getId());
        }
        response.setTitleSnapshot(item.getTitleSnapshot());
        response.setPriceSnapshot(item.getPriceSnapshot());
        response.setQuantity(item.getQuantity());
        if (item.getPriceSnapshot() != null) {
            response.setSubtotal(item.getPriceSnapshot().multiply(BigDecimal.valueOf(item.getQuantity())));
        }
        response.setUserRating(userRating);
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

    public String getTitleSnapshot() {
        return titleSnapshot;
    }

    public void setTitleSnapshot(String titleSnapshot) {
        this.titleSnapshot = titleSnapshot;
    }

    public BigDecimal getPriceSnapshot() {
        return priceSnapshot;
    }

    public void setPriceSnapshot(BigDecimal priceSnapshot) {
        this.priceSnapshot = priceSnapshot;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public BigDecimal getSubtotal() {
        return subtotal;
    }

    public void setSubtotal(BigDecimal subtotal) {
        this.subtotal = subtotal;
    }

    public Integer getUserRating() {
        return userRating;
    }

    public void setUserRating(Integer userRating) {
        this.userRating = userRating;
    }
}
