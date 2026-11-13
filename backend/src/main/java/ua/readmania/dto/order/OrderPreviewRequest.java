package ua.readmania.dto.order;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import java.util.List;

public class OrderPreviewRequest {

    private List<@Valid OrderItemRequest> items;

    @Min(value = 0, message = "Кількість бонусних балів не може бути від'ємною. Приклад: 0")
    private int bonusPoints = 0;

    public OrderPreviewRequest() {
    }

    public OrderPreviewRequest(List<OrderItemRequest> items, int bonusPoints) {
        this.items = items;
        this.bonusPoints = bonusPoints;
    }

    public List<OrderItemRequest> getItems() {
        return items;
    }

    public void setItems(List<OrderItemRequest> items) {
        this.items = items;
    }

    public int getBonusPoints() {
        return bonusPoints;
    }

    public void setBonusPoints(int bonusPoints) {
        this.bonusPoints = bonusPoints;
    }
}
