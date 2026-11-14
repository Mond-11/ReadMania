package ua.readmania.dto.manager.user;

import java.time.Instant;
import ua.readmania.domain.user.UserStatus;

public class ManagerUserSummaryResponse {

    private Long id;
    private String email;
    private UserStatus status;
    private Instant createdAt;
    private Instant lastLoginAt;

    public ManagerUserSummaryResponse() {
    }

    public ManagerUserSummaryResponse(Long id, String email, UserStatus status, Instant createdAt, Instant lastLoginAt) {
        this.id = id;
        this.email = email;
        this.status = status;
        this.createdAt = createdAt;
        this.lastLoginAt = lastLoginAt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public UserStatus getStatus() {
        return status;
    }

    public void setStatus(UserStatus status) {
        this.status = status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getLastLoginAt() {
        return lastLoginAt;
    }

    public void setLastLoginAt(Instant lastLoginAt) {
        this.lastLoginAt = lastLoginAt;
    }
}
