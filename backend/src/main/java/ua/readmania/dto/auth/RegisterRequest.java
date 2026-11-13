package ua.readmania.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class RegisterRequest {

    @NotBlank(message = "Email не може бути порожнім. Приклад: user@example.com")
    @Email(message = "Невірний формат email. Приклад: user@example.com")
    private String email;

    @NotBlank(message = "Пароль не може бути порожнім. Приклад: Pass123!")
    @Size(min = 6, message = "Пароль повинен містити щонайменше 6 символів")
    private String password;

    @NotBlank(message = "Підтвердження пароля обов'язкове")
    private String confirmPassword;

    private String fullName;

    public RegisterRequest() {
    }

    public RegisterRequest(String email, String password, String confirmPassword, String fullName) {
        this.email = email;
        this.password = password;
        this.confirmPassword = confirmPassword;
        this.fullName = fullName;
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

    public String getConfirmPassword() {
        return confirmPassword;
    }

    public void setConfirmPassword(String confirmPassword) {
        this.confirmPassword = confirmPassword;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }
}
