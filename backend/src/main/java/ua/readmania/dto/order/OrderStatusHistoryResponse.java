package ua.readmania.dto.order;

import java.time.Instant;
import ua.readmania.domain.order.OrderStatus;
import ua.readmania.domain.order.OrderStatusHistory;

public class OrderStatusHistoryResponse {

    private Long id;
    private OrderStatus fromStatus;
    private OrderStatus toStatus;
    private Instant changedAt;
    private String changedByUserName;
    private String comment;

    public OrderStatusHistoryResponse() {
    }

    public static OrderStatusHistoryResponse fromEntity(OrderStatusHistory history) {
        if (history == null) {
            return null;
        }
        OrderStatusHistoryResponse response = new OrderStatusHistoryResponse();
        response.setId(history.getId());
        response.setFromStatus(history.getFromStatus());
        response.setToStatus(history.getToStatus());
        response.setChangedAt(history.getChangedAt());
        if (history.getChangedByUser() != null) {
            response.setChangedByUserName(history.getChangedByUser().getFullName() != null
                    ? history.getChangedByUser().getFullName()
                    : history.getChangedByUser().getEmail());
        }
        response.setComment(history.getComment());
        return response;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public OrderStatus getFromStatus() {
        return fromStatus;
    }

    public void setFromStatus(OrderStatus fromStatus) {
        this.fromStatus = fromStatus;
    }

    public OrderStatus getToStatus() {
        return toStatus;
    }

    public void setToStatus(OrderStatus toStatus) {
        this.toStatus = toStatus;
    }

    public Instant getChangedAt() {
        return changedAt;
    }

    public void setChangedAt(Instant changedAt) {
        this.changedAt = changedAt;
    }

    public String getChangedByUserName() {
        return changedByUserName;
    }

    public void setChangedByUserName(String changedByUserName) {
        this.changedByUserName = changedByUserName;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }
}
