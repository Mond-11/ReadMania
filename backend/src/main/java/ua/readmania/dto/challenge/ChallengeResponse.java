package ua.readmania.dto.challenge;

import java.time.Instant;
import ua.readmania.domain.challenge.Challenge;
import ua.readmania.domain.challenge.ChallengeStatus;

public class ChallengeResponse {

    private Long id;
    private String name;
    private String description;
    private int requiredBooks;
    private Long genreId;
    private String genreName;
    private Instant deadline;
    private int rewardPoints;
    private ChallengeStatus status;
    private int userProgress;
    private boolean completed;
    private Instant completedAt;

    public ChallengeResponse() {
    }

    public static ChallengeResponse fromEntity(Challenge challenge, int userProgress, boolean completed, Instant completedAt) {
        if (challenge == null) {
            return null;
        }
        ChallengeResponse response = new ChallengeResponse();
        response.setId(challenge.getId());
        response.setName(challenge.getName());
        response.setDescription(challenge.getDescription());
        response.setRequiredBooks(challenge.getRequiredBooks());
        if (challenge.getGenre() != null) {
            response.setGenreId(challenge.getGenre().getId());
            response.setGenreName(challenge.getGenre().getName());
        }
        response.setDeadline(challenge.getDeadline());
        response.setRewardPoints(challenge.getRewardPoints());
        response.setStatus(challenge.getStatus());
        response.setUserProgress(userProgress);
        response.setCompleted(completed);
        response.setCompletedAt(completedAt);
        return response;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public int getRequiredBooks() {
        return requiredBooks;
    }

    public void setRequiredBooks(int requiredBooks) {
        this.requiredBooks = requiredBooks;
    }

    public Long getGenreId() {
        return genreId;
    }

    public void setGenreId(Long genreId) {
        this.genreId = genreId;
    }

    public String getGenreName() {
        return genreName;
    }

    public void setGenreName(String genreName) {
        this.genreName = genreName;
    }

    public Instant getDeadline() {
        return deadline;
    }

    public void setDeadline(Instant deadline) {
        this.deadline = deadline;
    }

    public int getRewardPoints() {
        return rewardPoints;
    }

    public void setRewardPoints(int rewardPoints) {
        this.rewardPoints = rewardPoints;
    }

    public ChallengeStatus getStatus() {
        return status;
    }

    public void setStatus(ChallengeStatus status) {
        this.status = status;
    }

    public int getUserProgress() {
        return userProgress;
    }

    public void setUserProgress(int userProgress) {
        this.userProgress = userProgress;
    }

    public boolean isCompleted() {
        return completed;
    }

    public void setCompleted(boolean completed) {
        this.completed = completed;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(Instant completedAt) {
        this.completedAt = completedAt;
    }
}
