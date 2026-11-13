package ua.readmania.dto.challenge;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;

public class UpdateChallengeRequest {

    @NotBlank(message = "Назва виклику є обов'язковою")
    private String title;

    private String description;

    @NotNull(message = "Кількість книг є обов'язковою")
    @Min(value = 1, message = "Кількість книг повинна бути більше 0")
    private Integer requiredBooks;

    private Long genreId;

    @NotNull(message = "Дедлайн є обов'язковим")
    @Future(message = "Дедлайн повинен бути в майбутньому")
    private Instant deadline;

    @NotNull(message = "Кількість бонусних балів є обов'язковою")
    @Min(value = 0, message = "Кількість бонусних балів не може бути від'ємною")
    private Integer rewardPoints;

    public UpdateChallengeRequest() {
    }

    public UpdateChallengeRequest(String title, String description, Integer requiredBooks, Long genreId, Instant deadline, Integer rewardPoints) {
        this.title = title;
        this.description = description;
        this.requiredBooks = requiredBooks;
        this.genreId = genreId;
        this.deadline = deadline;
        this.rewardPoints = rewardPoints;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getName() {
        return title;
    }

    public void setName(String name) {
        this.title = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Integer getRequiredBooks() {
        return requiredBooks;
    }

    public void setRequiredBooks(Integer requiredBooks) {
        this.requiredBooks = requiredBooks;
    }

    public Long getGenreId() {
        return genreId;
    }

    public void setGenreId(Long genreId) {
        this.genreId = genreId;
    }

    public Instant getDeadline() {
        return deadline;
    }

    public void setDeadline(Instant deadline) {
        this.deadline = deadline;
    }

    public Integer getRewardPoints() {
        return rewardPoints;
    }

    public void setRewardPoints(Integer rewardPoints) {
        this.rewardPoints = rewardPoints;
    }
}
