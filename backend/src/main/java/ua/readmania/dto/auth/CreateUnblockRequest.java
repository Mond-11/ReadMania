package ua.readmania.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class CreateUnblockRequest {

    @NotBlank(message = "Email обов'язковий")
    @Email(message = "Невірний формат email. Приклад: user@example.com")
    private String email;

    @NotBlank(message = "Пароль обов'язковий")
    private String password;

    @NotBlank(message = "Вкажіть причину для розблокування")
    @Size(max = 1000, message = "Причина не повинна перевищувати 1000 символів")
    private String reason;

    public CreateUnblockRequest() {
    }

    public CreateUnblockRequest(String email, String password, String reason) {
        this.email = email;
        this.password = password;
        this.reason = reason;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
