package ua.readmania.dto.cart;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class CartResponse {

    private List<CartItemResponse> items = new ArrayList<>();
    private int totalItems;
    private BigDecimal totalPrice = BigDecimal.ZERO;
    private boolean allAvailable = true;

    public CartResponse() {
    }

    public CartResponse(List<CartItemResponse> items, int totalItems, BigDecimal totalPrice, boolean allAvailable) {
        this.items = items != null ? items : new ArrayList<>();
        this.totalItems = totalItems;
        this.totalPrice = totalPrice != null ? totalPrice : BigDecimal.ZERO;
        this.allAvailable = allAvailable;
    }

    public List<CartItemResponse> getItems() {
        return items;
    }

    public void setItems(List<CartItemResponse> items) {
        this.items = items != null ? items : new ArrayList<>();
    }

    public int getTotalItems() {
        return totalItems;
    }

    public void setTotalItems(int totalItems) {
        this.totalItems = totalItems;
    }

    public BigDecimal getTotalPrice() {
        return totalPrice;
    }

    public void setTotalPrice(BigDecimal totalPrice) {
        this.totalPrice = totalPrice;
    }

    public boolean isAllAvailable() {
        return allAvailable;
    }

    public void setAllAvailable(boolean allAvailable) {
        this.allAvailable = allAvailable;
    }
}
