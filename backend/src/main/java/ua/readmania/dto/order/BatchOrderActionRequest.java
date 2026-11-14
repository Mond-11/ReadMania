package ua.readmania.dto.order;

import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public class BatchOrderActionRequest {

    @NotEmpty(message = "Список ідентифікаторів замовлень не може бути порожнім")
    private List<Long> orderIds;

    public BatchOrderActionRequest() {
    }

    public BatchOrderActionRequest(List<Long> orderIds) {
        this.orderIds = orderIds;
    }

    public List<Long> getOrderIds() {
        return orderIds;
    }

    public void setOrderIds(List<Long> orderIds) {
        this.orderIds = orderIds;
    }
}
