package ua.readmania.dto.cart;

import jakarta.validation.constraints.Min;

public class UpdateCartItemRequest {

    @Min(value = 1, message = "Кількість повинна бути щонайменше 1. Приклад: 1")
    private int quantity = 1;

    public UpdateCartItemRequest() {
    }

    public UpdateCartItemRequest(int quantity) {
        this.quantity = quantity;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }
}
