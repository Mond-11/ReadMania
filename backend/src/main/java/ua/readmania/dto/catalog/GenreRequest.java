package ua.readmania.dto.catalog;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class GenreRequest {

    @NotBlank(message = "Назва жанру не може бути порожньою. Приклад: Фентезі")
    @Size(max = 100, message = "Назва жанру не повинна перевищувати 100 символів")
    private String name;

    public GenreRequest() {
    }

    public GenreRequest(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
