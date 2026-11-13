package ua.readmania.dto.giftset;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.List;

public class GenerateGiftSetRequest {

    @NotNull(message = "Бюджет є обов'язковим")
    @DecimalMin(value = "50.00", message = "Мінімальний бюджет становить 50.00 грн")
    private BigDecimal budget;

    @NotEmpty(message = "Оберіть хоча б один жанр для формування набору")
    private List<Long> genreIds;

    @Min(value = 1, message = "Кількість книг повинна бути щонайменше 1")
    private Integer bookCount;

    public GenerateGiftSetRequest() {
    }

    public GenerateGiftSetRequest(BigDecimal budget, List<Long> genreIds, Integer bookCount) {
        this.budget = budget;
        this.genreIds = genreIds;
        this.bookCount = bookCount;
    }

    public BigDecimal getBudget() {
        return budget;
    }

    public void setBudget(BigDecimal budget) {
        this.budget = budget;
    }

    public List<Long> getGenreIds() {
        return genreIds;
    }

    public void setGenreIds(List<Long> genreIds) {
        this.genreIds = genreIds;
    }

    public Integer getBookCount() {
        return bookCount;
    }

    public void setBookCount(Integer bookCount) {
        this.bookCount = bookCount;
    }
}
