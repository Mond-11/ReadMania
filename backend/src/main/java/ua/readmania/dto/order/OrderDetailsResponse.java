package ua.readmania.dto.order;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import ua.readmania.domain.order.DeliveryMethod;
import ua.readmania.domain.order.Order;
import ua.readmania.domain.order.OrderStatus;
import ua.readmania.domain.order.OrderType;
import ua.readmania.domain.order.PaymentMethod;

public class OrderDetailsResponse {

    private Long id;
    private OrderType type;
    private OrderStatus status;
    private PaymentMethod paymentMethod;
    private DeliveryMethod deliveryMethod;
    private String address;
    private String recipientName;
    private String recipientPhone;
    private Long institutionId;
    private String institutionName;
    private BigDecimal itemsTotal;
    private BigDecimal discountAmount;
    private int pointsUsed;
    private BigDecimal totalToPay;
    private Long version;
    private Instant createdAt;
    private Instant statusChangedAt;
    private Instant deliveredAt;
    private List<OrderItemResponse> items = new ArrayList<>();
    private List<OrderStatusHistoryResponse> statusHistory = new ArrayList<>();

    public OrderDetailsResponse() {
    }

    public static OrderDetailsResponse fromEntity(Order order, List<OrderStatusHistoryResponse> statusHistory) {
        if (order == null) {
            return null;
        }
        OrderDetailsResponse response = new OrderDetailsResponse();
        response.setId(order.getId());
        response.setType(order.getType());
        response.setStatus(order.getStatus());
        response.setPaymentMethod(order.getPaymentMethod());
        response.setDeliveryMethod(order.getDeliveryMethod());
        response.setAddress(order.getAddress());
        response.setRecipientName(order.getRecipientName());
        response.setRecipientPhone(order.getRecipientPhone());
        if (order.getInstitution() != null) {
            response.setInstitutionId(order.getInstitution().getId());
        }
        response.setInstitutionName(order.getInstitutionName());
        response.setItemsTotal(order.getItemsTotal());
        response.setDiscountAmount(order.getDiscountAmount());
        response.setPointsUsed(order.getPointsUsed());
        response.setTotalToPay(order.getTotalToPay());
        response.setVersion(order.getVersion());
        response.setCreatedAt(order.getCreatedAt());
        response.setStatusChangedAt(order.getStatusChangedAt());
        response.setDeliveredAt(order.getDeliveredAt());
        if (order.getItems() != null) {
            response.setItems(order.getItems().stream().map(OrderItemResponse::fromEntity).toList());
        }
        response.setStatusHistory(statusHistory != null ? statusHistory : new ArrayList<>());
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

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getRecipientName() {
        return recipientName;
    }

    public void setRecipientName(String recipientName) {
        this.recipientName = recipientName;
    }

    public String getRecipientPhone() {
        return recipientPhone;
    }

    public void setRecipientPhone(String recipientPhone) {
        this.recipientPhone = recipientPhone;
    }

    public Long getInstitutionId() {
        return institutionId;
    }

    public void setInstitutionId(Long institutionId) {
        this.institutionId = institutionId;
    }

    public String getInstitutionName() {
        return institutionName;
    }

    public void setInstitutionName(String institutionName) {
        this.institutionName = institutionName;
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

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
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

    public List<OrderItemResponse> getItems() {
        return items;
    }

    public void setItems(List<OrderItemResponse> items) {
        this.items = items != null ? items : new ArrayList<>();
    }

    public List<OrderStatusHistoryResponse> getStatusHistory() {
        return statusHistory;
    }

    public void setStatusHistory(List<OrderStatusHistoryResponse> statusHistory) {
        this.statusHistory = statusHistory != null ? statusHistory : new ArrayList<>();
    }
}
