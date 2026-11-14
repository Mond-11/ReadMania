package ua.readmania.dto.manager.user;

import java.time.Instant;

public class ManagerUnblockRequestResponse {

    private Long requestId;
    private Long userId;
    private String email;
    private Instant blockedAt;
    private String blockReason;
    private String justification;
    private Instant createdAt;

    public ManagerUnblockRequestResponse() {
    }

    public ManagerUnblockRequestResponse(Long requestId, Long userId, String email, Instant blockedAt,
                                         String blockReason, String justification, Instant createdAt) {
        this.requestId = requestId;
        this.userId = userId;
        this.email = email;
        this.blockedAt = blockedAt;
        this.blockReason = blockReason;
        this.justification = justification;
        this.createdAt = createdAt;
    }

    public Long getRequestId() {
        return requestId;
    }

    public void setRequestId(Long requestId) {
        this.requestId = requestId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public Instant getBlockedAt() {
        return blockedAt;
    }

    public void setBlockedAt(Instant blockedAt) {
        this.blockedAt = blockedAt;
    }

    public String getBlockReason() {
        return blockReason;
    }

    public void setBlockReason(String blockReason) {
        this.blockReason = blockReason;
    }

    public String getJustification() {
        return justification;
    }

    public void setJustification(String justification) {
        this.justification = justification;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
