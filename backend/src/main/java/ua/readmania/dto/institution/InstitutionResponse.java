package ua.readmania.dto.institution;

import ua.readmania.domain.institution.Institution;
import ua.readmania.domain.institution.InstitutionType;

public class InstitutionResponse {

    private Long id;
    private String name;
    private String address;
    private String contactPerson;
    private InstitutionType type;

    public InstitutionResponse() {
    }

    public InstitutionResponse(Long id, String name, String address, String contactPerson, InstitutionType type) {
        this.id = id;
        this.name = name;
        this.address = address;
        this.contactPerson = contactPerson;
        this.type = type;
    }

    public static InstitutionResponse fromEntity(Institution institution) {
        if (institution == null) {
            return null;
        }
        return new InstitutionResponse(
                institution.getId(),
                institution.getName(),
                institution.getAddress(),
                institution.getContactPerson(),
                institution.getType()
        );
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
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

    public InstitutionType getType() {
        return type;
    }

    public void setType(InstitutionType type) {
        this.type = type;
    }
}
