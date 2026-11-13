package ua.readmania.dto.loyalty;

import java.time.Instant;
import ua.readmania.domain.loyalty.BonusTransaction;
import ua.readmania.domain.loyalty.BonusTransactionType;

public class BonusTransactionResponse {

    private Long id;
    private int points;
    private BonusTransactionType type;
    private Long orderId;
    private Long challengeId;
    private String challengeName;
    private Instant createdAt;

    public BonusTransactionResponse() {
    }

    public static BonusTransactionResponse fromEntity(BonusTransaction tx) {
        if (tx == null) {
            return null;
        }
        BonusTransactionResponse response = new BonusTransactionResponse();
        response.setId(tx.getId());
        response.setPoints(tx.getPoints());
        response.setType(tx.getType());
        if (tx.getOrder() != null) {
            response.setOrderId(tx.getOrder().getId());
        }
        if (tx.getChallenge() != null) {
            response.setChallengeId(tx.getChallenge().getId());
            response.setChallengeName(tx.getChallenge().getName());
        }
        response.setCreatedAt(tx.getCreatedAt());
        return response;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public int getPoints() {
        return points;
    }

    public void setPoints(int points) {
        this.points = points;
    }

    public BonusTransactionType getType() {
        return type;
    }

    public void setType(BonusTransactionType type) {
        this.type = type;
    }

    public Long getOrderId() {
        return orderId;
    }

    public void setOrderId(Long orderId) {
        this.orderId = orderId;
    }

    public Long getChallengeId() {
        return challengeId;
    }

    public void setChallengeId(Long challengeId) {
        this.challengeId = challengeId;
    }

    public String getChallengeName() {
        return challengeName;
    }

    public void setChallengeName(String challengeName) {
        this.challengeName = challengeName;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
