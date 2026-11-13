package ua.readmania.dto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class PasswordResetConfirmRequest {

    @NotBlank(message = "Токен обов'язковий")
    private String token;

    @NotBlank(message = "Новий пароль не може бути порожнім")
    @Size(min = 6, message = "Пароль повинен містити щонайменше 6 символів")
    private String newPassword;

    @NotBlank(message = "Підтвердження нового пароля обов'язкове")
    private String confirmPassword;

    public PasswordResetConfirmRequest() {
    }

    public PasswordResetConfirmRequest(String token, String newPassword, String confirmPassword) {
        this.token = token;
        this.newPassword = newPassword;
        this.confirmPassword = confirmPassword;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
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
