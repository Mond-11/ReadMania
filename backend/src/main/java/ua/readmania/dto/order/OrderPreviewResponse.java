package ua.readmania.dto.order;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class OrderPreviewResponse {

    private BigDecimal itemsTotal = BigDecimal.ZERO;
    private int userBonusBalance;
    private int maxBonusPoints;
    private int pointsUsed;
    private BigDecimal discountAmount = BigDecimal.ZERO;
    private BigDecimal totalToPay = BigDecimal.ZERO;
    private List<OrderItemPreviewDto> items = new ArrayList<>();

    public OrderPreviewResponse() {
    }

    public static class OrderItemPreviewDto {
        private Long bookId;
        private String title;
        private BigDecimal price;
        private int quantity;
        private BigDecimal subtotal;
        private boolean available;

        public OrderItemPreviewDto() {
        }

        public OrderItemPreviewDto(Long bookId, String title, BigDecimal price, int quantity, BigDecimal subtotal, boolean available) {
            this.bookId = bookId;
            this.title = title;
            this.price = price;
            this.quantity = quantity;
            this.subtotal = subtotal;
            this.available = available;
        }

        public Long getBookId() {
            return bookId;
        }

        public void setBookId(Long bookId) {
            this.bookId = bookId;
        }

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public BigDecimal getPrice() {
            return price;
        }

        public void setPrice(BigDecimal price) {
            this.price = price;
        }

        public int getQuantity() {
            return quantity;
        }

        public void setQuantity(int quantity) {
            this.quantity = quantity;
        }

        public BigDecimal getSubtotal() {
            return subtotal;
        }

        public void setSubtotal(BigDecimal subtotal) {
            this.subtotal = subtotal;
        }

        public boolean isAvailable() {
            return available;
        }

        public void setAvailable(boolean available) {
            this.available = available;
        }
    }

    public BigDecimal getItemsTotal() {
        return itemsTotal;
    }

    public void setItemsTotal(BigDecimal itemsTotal) {
        this.itemsTotal = itemsTotal;
    }

    public int getUserBonusBalance() {
        return userBonusBalance;
    }

    public void setUserBonusBalance(int userBonusBalance) {
        this.userBonusBalance = userBonusBalance;
    }

    public int getMaxBonusPoints() {
        return maxBonusPoints;
    }

    public void setMaxBonusPoints(int maxBonusPoints) {
        this.maxBonusPoints = maxBonusPoints;
    }

    public int getPointsUsed() {
        return pointsUsed;
    }

    public void setPointsUsed(int pointsUsed) {
        this.pointsUsed = pointsUsed;
    }

    public BigDecimal getDiscountAmount() {
        return discountAmount;
    }

    public void setDiscountAmount(BigDecimal discountAmount) {
        this.discountAmount = discountAmount;
    }

    public BigDecimal getTotalToPay() {
        return totalToPay;
    }

    public void setTotalToPay(BigDecimal totalToPay) {
        this.totalToPay = totalToPay;
    }

    public List<OrderItemPreviewDto> getItems() {
        return items;
    }

    public void setItems(List<OrderItemPreviewDto> items) {
        this.items = items != null ? items : new ArrayList<>();
    }
}
