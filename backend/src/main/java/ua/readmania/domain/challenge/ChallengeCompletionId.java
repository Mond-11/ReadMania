package ua.readmania.domain.challenge;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class ChallengeCompletionId implements Serializable {

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "challenge_id")
    private Long challengeId;

    public ChallengeCompletionId() {
    }

    public ChallengeCompletionId(Long userId, Long challengeId) {
        this.userId = userId;
        this.challengeId = challengeId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getChallengeId() {
        return challengeId;
    }

    public void setChallengeId(Long challengeId) {
        this.challengeId = challengeId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ChallengeCompletionId that)) return false;
        return Objects.equals(userId, that.userId) && Objects.equals(challengeId, that.challengeId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId, challengeId);
    }
}
