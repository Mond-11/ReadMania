package ua.readmania.dto.manager.user;

import java.time.Instant;
import ua.readmania.domain.user.UserStatus;

public class InactiveUserResponse {

    private Long id;
    private String email;
    private UserStatus status;
    private Instant referenceDate;

    public InactiveUserResponse() {
    }

    public InactiveUserResponse(Long id, String email, UserStatus status, Instant referenceDate) {
        this.id = id;
        this.email = email;
        this.status = status;
        this.referenceDate = referenceDate;
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

    public Instant getReferenceDate() {
        return referenceDate;
    }

    public void setReferenceDate(Instant referenceDate) {
        this.referenceDate = referenceDate;
    }
}
