package ua.readmania.dto.giftset;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.List;

public class ReplaceGiftSetBookRequest {

    @NotEmpty(message = "Список поточних книг у наборі є обов'язковим")
    private List<Long> currentBookIds;

    @NotNull(message = "ID книги для заміни є обов'язковим")
    private Long bookIdToReplace;

    @NotEmpty(message = "Оберіть жанри для підбору заміни")
    private List<Long> genreIds;

    @NotNull(message = "Бюджет є обов'язковим")
    private BigDecimal budget;

    public ReplaceGiftSetBookRequest() {
    }

    public ReplaceGiftSetBookRequest(List<Long> currentBookIds, Long bookIdToReplace, List<Long> genreIds, BigDecimal budget) {
        this.currentBookIds = currentBookIds;
        this.bookIdToReplace = bookIdToReplace;
        this.genreIds = genreIds;
        this.budget = budget;
    }

    public List<Long> getCurrentBookIds() {
        return currentBookIds;
    }

    public void setCurrentBookIds(List<Long> currentBookIds) {
        this.currentBookIds = currentBookIds;
    }

    public Long getBookIdToReplace() {
        return bookIdToReplace;
    }

    public void setBookIdToReplace(Long bookIdToReplace) {
        this.bookIdToReplace = bookIdToReplace;
    }

    public List<Long> getGenreIds() {
        return genreIds;
    }

    public void setGenreIds(List<Long> genreIds) {
        this.genreIds = genreIds;
    }

    public BigDecimal getBudget() {
        return budget;
    }

    public void setBudget(BigDecimal budget) {
        this.budget = budget;
    }
}
