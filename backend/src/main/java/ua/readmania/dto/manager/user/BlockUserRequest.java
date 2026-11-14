package ua.readmania.dto.manager.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class BlockUserRequest {

    @NotBlank(message = "Причина блокування є обов'язковою")
    @Size(max = 500, message = "Причина блокування не може перевищувати 500 символів")
    private String reason;

    public BlockUserRequest() {
    }

    public BlockUserRequest(String reason) {
        this.reason = reason;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
