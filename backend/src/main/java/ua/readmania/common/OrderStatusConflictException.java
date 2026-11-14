package ua.readmania.common;

import ua.readmania.domain.order.OrderStatus;

public class OrderStatusConflictException extends RuntimeException {

    private final OrderStatus currentStatus;

    public OrderStatusConflictException(String message, OrderStatus currentStatus) {
        super(message);
        this.currentStatus = currentStatus;
    }

    public OrderStatusConflictException(String message, OrderStatus currentStatus, Throwable cause) {
        super(message, cause);
        this.currentStatus = currentStatus;
    }

    public OrderStatus getCurrentStatus() {
        return currentStatus;
    }
}
