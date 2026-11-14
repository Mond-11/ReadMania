package ua.readmania.dto.manager.user;

import java.time.Instant;
import java.util.Map;
import ua.readmania.domain.order.OrderStatus;
import ua.readmania.domain.user.UserStatus;

public class ManagerUserDetailsResponse {

    private Long id;
    private String email;
    private UserStatus status;
    private Instant createdAt;
    private Instant lastLoginAt;
    private int bonusBalance;
    private long totalOrders;
    private Map<OrderStatus, Long> ordersByStatus;
    private Instant blockedAt;
    private String blockReason;

    public ManagerUserDetailsResponse() {
    }

    public ManagerUserDetailsResponse(Long id, String email, UserStatus status, Instant createdAt,
                                      Instant lastLoginAt, int bonusBalance, long totalOrders,
                                      Map<OrderStatus, Long> ordersByStatus, Instant blockedAt, String blockReason) {
        this.id = id;
        this.email = email;
        this.status = status;
        this.createdAt = createdAt;
        this.lastLoginAt = lastLoginAt;
        this.bonusBalance = bonusBalance;
        this.totalOrders = totalOrders;
        this.ordersByStatus = ordersByStatus;
        this.blockedAt = blockedAt;
        this.blockReason = blockReason;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public UserStatus getStatus() {
        return status;
    }

    public void setStatus(UserStatus status) {
        this.status = status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getLastLoginAt() {
        return lastLoginAt;
    }

    public void setLastLoginAt(Instant lastLoginAt) {
        this.lastLoginAt = lastLoginAt;
    }

    public int getBonusBalance() {
        return bonusBalance;
    }

    public void setBonusBalance(int bonusBalance) {
        this.bonusBalance = bonusBalance;
    }

    public long getTotalOrders() {
        return totalOrders;
    }

    public void setTotalOrders(long totalOrders) {
        this.totalOrders = totalOrders;
    }

    public Map<OrderStatus, Long> getOrdersByStatus() {
        return ordersByStatus;
    }

    public void setOrdersByStatus(Map<OrderStatus, Long> ordersByStatus) {
        this.ordersByStatus = ordersByStatus;
    }

    public Instant getBlockedAt() {
        return blockedAt;
    }

    public void setBlockedAt(Instant blockedAt) {
        this.blockedAt = blockedAt;
    }

    public String getBlockReason() {
        return blockReason;
    }

    public void setBlockReason(String blockReason) {
        this.blockReason = blockReason;
    }
}
