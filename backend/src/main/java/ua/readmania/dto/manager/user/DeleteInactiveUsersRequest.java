package ua.readmania.dto.manager.user;

import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public class DeleteInactiveUsersRequest {

    @NotEmpty(message = "Список ідентифікаторів користувачів не може бути порожнім")
    private List<Long> userIds;

    public DeleteInactiveUsersRequest() {
    }

    public DeleteInactiveUsersRequest(List<Long> userIds) {
        this.userIds = userIds;
    }

    public List<Long> getUserIds() {
        return userIds;
    }

    public void setUserIds(List<Long> userIds) {
        this.userIds = userIds;
    }
}
