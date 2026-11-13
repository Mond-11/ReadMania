package ua.readmania.dto.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class ChangePasswordRequest {

    @NotBlank(message = "Поточний пароль обов'язковий")
    private String currentPassword;

    @NotBlank(message = "Новий пароль не може бути порожнім. Приклад: Pass123!")
    @Size(min = 6, message = "Пароль повинен містити щонайменше 6 символів")
    private String newPassword;

    @NotBlank(message = "Підтвердження нового пароля обов'язкове")
    private String confirmPassword;

    public ChangePasswordRequest() {
    }

    public ChangePasswordRequest(String currentPassword, String newPassword, String confirmPassword) {
        this.currentPassword = currentPassword;
        this.newPassword = newPassword;
        this.confirmPassword = confirmPassword;
    }

    public String getCurrentPassword() {
        return currentPassword;
    }

    public void setCurrentPassword(String currentPassword) {
        this.currentPassword = currentPassword;
    }

    public String getNewPassword() {
        return newPassword;
    }

    public void setNewPassword(String newPassword) {
        this.newPassword = newPassword;
    }

    public String getConfirmPassword() {
        return confirmPassword;
    }

    public void setConfirmPassword(String confirmPassword) {
        this.confirmPassword = confirmPassword;
    }
}
