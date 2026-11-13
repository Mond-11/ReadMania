package ua.readmania.dto.auth;

import java.time.Instant;
import ua.readmania.domain.user.Role;
import ua.readmania.domain.user.User;
import ua.readmania.domain.user.UserStatus;

public class UserResponse {

    private Long id;
    private String email;
    private String fullName;
    private Role role;
    private UserStatus status;
    private String phone;
    private String deliveryAddress;
    private int bonusBalance;
    private Instant createdAt;
    private Instant lastLoginAt;

    public UserResponse() {
    }

    public static UserResponse fromEntity(User user) {
        if (user == null) {
            return null;
        }
        UserResponse response = new UserResponse();
        response.setId(user.getId());
        response.setEmail(user.getEmail());
        response.setFullName(user.getFullName());
        response.setRole(user.getRole());
        response.setStatus(user.getStatus());
        response.setPhone(user.getPhone());
        response.setDeliveryAddress(user.getDeliveryAddress());
        response.setBonusBalance(user.getBonusBalance());
        response.setCreatedAt(user.getCreatedAt());
        response.setLastLoginAt(user.getLastLoginAt());
        return response;
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

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public UserStatus getStatus() {
        return status;
    }

    public void setStatus(UserStatus status) {
        this.status = status;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getDeliveryAddress() {
        return deliveryAddress;
    }

    public void setDeliveryAddress(String deliveryAddress) {
        this.deliveryAddress = deliveryAddress;
    }

    public int getBonusBalance() {
        return bonusBalance;
    }

    public void setBonusBalance(int bonusBalance) {
        this.bonusBalance = bonusBalance;
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
}
