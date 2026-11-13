package ua.readmania.dto.order;

import jakarta.validation.constraints.NotNull;
import ua.readmania.domain.order.OrderStatus;

public class UpdateOrderStatusRequest {

    private OrderStatus expectedStatus;

    @NotNull(message = "Новий статус замовлення є обов'язковим. Допустимі значення: PROCESSING, ASSEMBLED, DELIVERING, DELIVERED, CANCELLED")
    private OrderStatus status;

    private String comment;

    public UpdateOrderStatusRequest() {
    }

    public UpdateOrderStatusRequest(OrderStatus expectedStatus, OrderStatus status, String comment) {
        this.expectedStatus = expectedStatus;
        this.status = status;
        this.comment = comment;
    }

    public OrderStatus getExpectedStatus() {
        return expectedStatus;
    }

    public void setExpectedStatus(OrderStatus expectedStatus) {
        this.expectedStatus = expectedStatus;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public void setStatus(OrderStatus status) {
        this.status = status;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }
}
