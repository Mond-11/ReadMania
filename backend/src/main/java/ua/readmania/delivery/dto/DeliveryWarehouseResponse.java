package ua.readmania.delivery.dto;

public class DeliveryWarehouseResponse {

    private String ref;
    private String number;
    private String label;
    private String formattedAddress;

    public DeliveryWarehouseResponse() {
    }

    public DeliveryWarehouseResponse(String ref, String number, String label, String formattedAddress) {
        this.ref = ref;
        this.number = number;
        this.label = label;
        this.formattedAddress = formattedAddress;
    }

    public String getRef() {
        return ref;
    }

    public void setRef(String ref) {
        this.ref = ref;
    }

    public String getNumber() {
        return number;
    }

    public void setNumber(String number) {
        this.number = number;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getFormattedAddress() {
        return formattedAddress;
    }

    public void setFormattedAddress(String formattedAddress) {
        this.formattedAddress = formattedAddress;
    }
}
