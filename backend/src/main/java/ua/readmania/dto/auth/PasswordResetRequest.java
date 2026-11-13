package ua.readmania.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public class PasswordResetRequest {

    @NotBlank(message = "Email не може бути порожнім. Приклад: user@example.com")
    @Email(message = "Невірний формат email. Приклад: user@example.com")
    private String email;

    public PasswordResetRequest() {
    }

    public PasswordResetRequest(String email) {
        this.email = email;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }
}
