package ua.readmania.dto.institution;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import ua.readmania.domain.institution.InstitutionType;

public class InstitutionUpsertRequest {

    @NotBlank(message = "Назва закладу є обов'язковою")
    private String name;

    @NotNull(message = "Тип закладу є обов'язковим")
    private InstitutionType type;

    @NotBlank(message = "Адреса закладу є обов'язковою")
    private String address;

    @NotBlank(message = "Контактна особа є обов'язковою")
    private String contactPerson;

    public InstitutionUpsertRequest() {
    }

    public InstitutionUpsertRequest(String name, InstitutionType type, String address, String contactPerson) {
        this.name = name;
        this.type = type;
        this.address = address;
        this.contactPerson = contactPerson;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public InstitutionType getType() {
        return type;
    }

    public void setType(InstitutionType type) {
        this.type = type;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getContactPerson() {
        return contactPerson;
    }

    public void setContactPerson(String contactPerson) {
        this.contactPerson = contactPerson;
    }
}
