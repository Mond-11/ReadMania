package ua.readmania.domain.order;

public enum OrderStatus {
    AWAITING_PAYMENT,
    PROCESSING,
    ASSEMBLED,
    DELIVERING,
    DELIVERED,
    CANCELLED
}
