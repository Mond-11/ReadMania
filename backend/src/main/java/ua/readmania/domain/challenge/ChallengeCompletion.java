package ua.readmania.domain.challenge;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Objects;
import ua.readmania.domain.user.User;

@Entity
@Table(name = "challenge_completions")
public class ChallengeCompletion {

    @EmbeddedId
    private ChallengeCompletionId id = new ChallengeCompletionId();

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId("userId")
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId("challengeId")
    @JoinColumn(name = "challenge_id")
    private Challenge challenge;

    @Column(name = "completed_at", nullable = false, updatable = false)
    private Instant completedAt = Instant.now();

    public ChallengeCompletion() {
    }

    public ChallengeCompletion(User user, Challenge challenge) {
        this(user, challenge, Instant.now());
    }

    public ChallengeCompletion(User user, Challenge challenge, Instant completedAt) {
        this.user = user;
        this.challenge = challenge;
        this.completedAt = completedAt != null ? completedAt : Instant.now();
        if (user != null && user.getId() != null) {
            this.id.setUserId(user.getId());
        }
        if (challenge != null && challenge.getId() != null) {
            this.id.setChallengeId(challenge.getId());
        }
    }

    public ChallengeCompletionId getId() {
        return id;
    }

    public void setId(ChallengeCompletionId id) {
        this.id = id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
        if (user != null && user.getId() != null) {
            this.id.setUserId(user.getId());
        }
    }

    public Challenge getChallenge() {
        return challenge;
    }

    public void setChallenge(Challenge challenge) {
        this.challenge = challenge;
        if (challenge != null && challenge.getId() != null) {
            this.id.setChallengeId(challenge.getId());
        }
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(Instant completedAt) {
        this.completedAt = completedAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ChallengeCompletion that)) return false;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
