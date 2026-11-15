package ua.readmania.delivery.dto;

public class DeliveryCityResponse {

    private String ref;
    private String name;
    private String area;
    private String displayName;

    public DeliveryCityResponse() {
    }

    public DeliveryCityResponse(String ref, String name, String area, String displayName) {
        this.ref = ref;
        this.name = name;
        this.area = area;
        this.displayName = displayName;
    }

    public String getRef() {
        return ref;
    }

    public void setRef(String ref) {
        this.ref = ref;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getArea() {
        return area;
    }

    public void setArea(String area) {
        this.area = area;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }
}
