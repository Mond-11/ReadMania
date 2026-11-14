package ua.readmania.common;

import java.time.Instant;

public class AccountBlockedException extends RuntimeException {

    private final String blockReason;
    private final Instant blockedAt;

    public AccountBlockedException(String message) {
        this(message, null, null);
    }

    public AccountBlockedException(String message, String blockReason, Instant blockedAt) {
        super(message);
        this.blockReason = blockReason;
        this.blockedAt = blockedAt;
    }

    public String getBlockReason() {
        return blockReason;
    }

    public Instant getBlockedAt() {
        return blockedAt;
    }
}
