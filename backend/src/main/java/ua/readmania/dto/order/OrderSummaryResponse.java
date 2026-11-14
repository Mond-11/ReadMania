package ua.readmania.dto.order;

import java.math.BigDecimal;
import java.time.Instant;
import ua.readmania.domain.order.DeliveryMethod;
import ua.readmania.domain.order.Order;
import ua.readmania.domain.order.OrderStatus;
import ua.readmania.domain.order.OrderType;
import ua.readmania.domain.order.PaymentMethod;

public class OrderSummaryResponse {

    private Long id;
    private OrderType type;
    private OrderStatus status;
    private PaymentMethod paymentMethod;
    private DeliveryMethod deliveryMethod;
    private BigDecimal itemsTotal;
    private BigDecimal discountAmount;
    private int pointsUsed;
    private BigDecimal totalToPay;
    private String institutionName;
    private String institutionContactPerson;
    private int itemsCount;
    private Instant createdAt;
    private Instant statusChangedAt;
    private Instant deliveredAt;

    public OrderSummaryResponse() {
    }

    public static OrderSummaryResponse fromEntity(Order order) {
        if (order == null) {
            return null;
        }
        OrderSummaryResponse response = new OrderSummaryResponse();
        response.setId(order.getId());
        response.setType(order.getType());
        response.setStatus(order.getStatus());
        response.setPaymentMethod(order.getPaymentMethod());
        response.setDeliveryMethod(order.getDeliveryMethod());
        response.setItemsTotal(order.getItemsTotal());
        response.setDiscountAmount(order.getDiscountAmount());
        response.setPointsUsed(order.getPointsUsed());
        response.setTotalToPay(order.getTotalToPay());
        response.setInstitutionName(order.getInstitutionName());
        response.setInstitutionContactPerson(order.getInstitutionContactPerson());
        if (order.getItems() != null) {
            response.setItemsCount(order.getItems().stream().mapToInt(item -> item.getQuantity()).sum());
        }
        response.setCreatedAt(order.getCreatedAt());
        response.setStatusChangedAt(order.getStatusChangedAt());
        response.setDeliveredAt(order.getDeliveredAt());
        return response;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public OrderType getType() {
        return type;
    }

    public void setType(OrderType type) {
        this.type = type;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public void setStatus(OrderStatus status) {
        this.status = status;
    }

    public PaymentMethod getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(PaymentMethod paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public DeliveryMethod getDeliveryMethod() {
        return deliveryMethod;
    }

    public void setDeliveryMethod(DeliveryMethod deliveryMethod) {
        this.deliveryMethod = deliveryMethod;
    }

    public BigDecimal getItemsTotal() {
        return itemsTotal;
    }

    public void setItemsTotal(BigDecimal itemsTotal) {
        this.itemsTotal = itemsTotal;
    }

    public BigDecimal getDiscountAmount() {
        return discountAmount;
    }

    public void setDiscountAmount(BigDecimal discountAmount) {
        this.discountAmount = discountAmount;
    }

    public int getPointsUsed() {
        return pointsUsed;
    }

    public void setPointsUsed(int pointsUsed) {
        this.pointsUsed = pointsUsed;
    }

    public BigDecimal getTotalToPay() {
        return totalToPay;
    }

    public void setTotalToPay(BigDecimal totalToPay) {
        this.totalToPay = totalToPay;
    }

    public String getInstitutionName() {
        return institutionName;
    }

    public void setInstitutionName(String institutionName) {
        this.institutionName = institutionName;
    }

    public String getInstitutionContactPerson() {
        return institutionContactPerson;
    }

    public void setInstitutionContactPerson(String institutionContactPerson) {
        this.institutionContactPerson = institutionContactPerson;
    }

    public int getItemsCount() {
        return itemsCount;
    }

    public void setItemsCount(int itemsCount) {
        this.itemsCount = itemsCount;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getStatusChangedAt() {
        return statusChangedAt;
    }

    public void setStatusChangedAt(Instant statusChangedAt) {
        this.statusChangedAt = statusChangedAt;
    }

    public Instant getDeliveredAt() {
        return deliveredAt;
    }

    public void setDeliveredAt(Instant deliveredAt) {
        this.deliveredAt = deliveredAt;
    }
}
