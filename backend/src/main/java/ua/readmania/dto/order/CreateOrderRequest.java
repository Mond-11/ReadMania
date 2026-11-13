package ua.readmania.dto.order;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import ua.readmania.domain.order.DeliveryMethod;
import ua.readmania.domain.order.PaymentMethod;

public class CreateOrderRequest {

    private List<@Valid OrderItemRequest> items;

    @NotNull(message = "Спосіб доставки є обов'язковим. Допустимі значення: COURIER, POST_OFFICE")
    private DeliveryMethod deliveryMethod;

    @NotBlank(message = "Адреса доставки є обов'язковою. Приклад: м. Київ, вул. Хрещатик, буд. 1, кв. 10")
    private String address;

    @NotNull(message = "Контактні дані одержувача є обов'язковими")
    @Valid
    private OrderContactDto contacts;

    @NotNull(message = "Спосіб оплати є обов'язковим. Допустимі значення: ON_DELIVERY, TRANSFER")
    private PaymentMethod paymentMethod;

    @Min(value = 0, message = "Кількість бонусних балів не може бути від'ємною. Приклад: 0")
    private int bonusPoints = 0;

    public CreateOrderRequest() {
    }

    public List<OrderItemRequest> getItems() {
        return items;
    }

    public void setItems(List<OrderItemRequest> items) {
        this.items = items;
    }

    public DeliveryMethod getDeliveryMethod() {
        return deliveryMethod;
    }

    public void setDeliveryMethod(DeliveryMethod deliveryMethod) {
        this.deliveryMethod = deliveryMethod;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public OrderContactDto getContacts() {
        return contacts;
    }

    public void setContacts(OrderContactDto contacts) {
        this.contacts = contacts;
    }

    public PaymentMethod getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(PaymentMethod paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public int getBonusPoints() {
        return bonusPoints;
    }

    public void setBonusPoints(int bonusPoints) {
        this.bonusPoints = bonusPoints;
    }
}
