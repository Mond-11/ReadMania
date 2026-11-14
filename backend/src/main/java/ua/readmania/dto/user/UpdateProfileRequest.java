package ua.readmania.dto.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public class UpdateProfileRequest {

    private String fullName;
    private String phone;
    private String deliveryAddress;

    @Email(message = "Невірний формат email. Приклад: user@example.com")
    private String email;

    @NotBlank(message = "Для зміни даних профілю необхідно обов'язково вказати поточний пароль")
    private String currentPassword;

    public UpdateProfileRequest() {
    }

    public UpdateProfileRequest(String fullName, String phone, String deliveryAddress, String email, String currentPassword) {
        this.fullName = fullName;
        this.phone = phone;
        this.deliveryAddress = deliveryAddress;
        this.email = email;
        this.currentPassword = currentPassword;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
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

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getCurrentPassword() {
        return currentPassword;
    }

    public void setCurrentPassword(String currentPassword) {
        this.currentPassword = currentPassword;
    }
}
