package ua.readmania.dto.order;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public class OrderContactDto {

    @NotBlank(message = "Ім'я одержувача є обов'язковим. Приклад: Іван Франко")
    private String recipientName;

    @NotBlank(message = "Телефон одержувача є обов'язковим. Приклад: +380501234567")
    @Pattern(regexp = "^\\+?[0-9]{10,15}$", message = "Невірний формат номера телефону. Приклад: +380501234567")
    private String phone;

    public OrderContactDto() {
    }

    public OrderContactDto(String recipientName, String phone) {
        this.recipientName = recipientName;
        this.phone = phone;
    }

    public String getRecipientName() {
        return recipientName;
    }

    public void setRecipientName(String recipientName) {
        this.recipientName = recipientName;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }
}
