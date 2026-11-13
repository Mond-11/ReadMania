package ua.readmania.dto.challenge;

import java.time.Instant;
import ua.readmania.domain.challenge.ChallengeStatus;

public class FinishChallengeResponse {

    private Long id;
    private String title;
    private ChallengeStatus status;
    private Instant finishedAt;
    private int notifiedUsersCount;
    private String message;

    public FinishChallengeResponse() {
    }

    public FinishChallengeResponse(Long id, String title, ChallengeStatus status, Instant finishedAt, int notifiedUsersCount, String message) {
        this.id = id;
        this.title = title;
        this.status = status;
        this.finishedAt = finishedAt;
        this.notifiedUsersCount = notifiedUsersCount;
        this.message = message;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public ChallengeStatus getStatus() {
        return status;
    }

    public void setStatus(ChallengeStatus status) {
        this.status = status;
    }

    public Instant getFinishedAt() {
        return finishedAt;
    }

    public void setFinishedAt(Instant finishedAt) {
        this.finishedAt = finishedAt;
    }

    public int getNotifiedUsersCount() {
        return notifiedUsersCount;
    }

    public void setNotifiedUsersCount(int notifiedUsersCount) {
        this.notifiedUsersCount = notifiedUsersCount;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
