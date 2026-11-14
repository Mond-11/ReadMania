package ua.readmania.dto.institution;

import ua.readmania.domain.institution.Institution;
import ua.readmania.domain.institution.InstitutionType;

public class InstitutionDetailsResponse {

    private Long id;
    private String name;
    private InstitutionType type;
    private String address;
    private String contactPerson;
    private long giftOrdersCount;
    private boolean canDelete;

    public InstitutionDetailsResponse() {
    }

    public InstitutionDetailsResponse(Long id, String name, InstitutionType type, String address,
                                      String contactPerson, long giftOrdersCount, boolean canDelete) {
        this.id = id;
        this.name = name;
        this.type = type;
        this.address = address;
        this.contactPerson = contactPerson;
        this.giftOrdersCount = giftOrdersCount;
        this.canDelete = canDelete;
    }

    public static InstitutionDetailsResponse of(Institution institution, long giftOrdersCount, boolean canDelete) {
        if (institution == null) {
            return null;
        }
        return new InstitutionDetailsResponse(
                institution.getId(),
                institution.getName(),
                institution.getType(),
                institution.getAddress(),
                institution.getContactPerson(),
                giftOrdersCount,
                canDelete
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

    public long getGiftOrdersCount() {
        return giftOrdersCount;
    }

    public void setGiftOrdersCount(long giftOrdersCount) {
        this.giftOrdersCount = giftOrdersCount;
    }

    public boolean isCanDelete() {
        return canDelete;
    }

    public void setCanDelete(boolean canDelete) {
        this.canDelete = canDelete;
    }
}
