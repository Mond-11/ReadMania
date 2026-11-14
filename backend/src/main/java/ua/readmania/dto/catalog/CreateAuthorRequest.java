package ua.readmania.dto.catalog;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class CreateAuthorRequest {

    @NotBlank(message = "Ім'я автора не може бути порожнім. Приклад: Тарас Шевченко")
    @Size(max = 255, message = "Ім'я автора не повинно перевищувати 255 символів")
    @JsonAlias({"name", "fullName"})
    private String fullName;

    public CreateAuthorRequest() {
    }

    public CreateAuthorRequest(String fullName) {
        this.fullName = fullName;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }
}
