package ua.readmania.dto.institution;

import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public class DeleteInstitutionsRequest {

    @NotEmpty(message = "Список ідентифікаторів закладів не може бути порожнім")
    private List<Long> institutionIds;

    public DeleteInstitutionsRequest() {
    }

    public DeleteInstitutionsRequest(List<Long> institutionIds) {
        this.institutionIds = institutionIds;
    }

    public List<Long> getInstitutionIds() {
        return institutionIds;
    }

    public void setInstitutionIds(List<Long> institutionIds) {
        this.institutionIds = institutionIds;
    }
}
