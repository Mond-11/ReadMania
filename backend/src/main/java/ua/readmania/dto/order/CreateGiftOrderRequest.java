package ua.readmania.dto.order;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import ua.readmania.domain.order.PaymentMethod;

public class CreateGiftOrderRequest {

    @NotNull(message = "Ідентифікатор закладу є обов'язковим. Приклад: 1")
    private Long institutionId;

    private List<@Valid OrderItemRequest> items;

    @NotNull(message = "Спосіб оплати є обов'язковим. Для подарунків дозволено лише TRANSFER")
    private PaymentMethod paymentMethod = PaymentMethod.TRANSFER;

    @Min(value = 0, message = "Кількість бонусних балів не може бути від'ємною. Приклад: 0")
    private int bonusPoints = 0;

    public CreateGiftOrderRequest() {
    }

    public CreateGiftOrderRequest(Long institutionId, List<OrderItemRequest> items, PaymentMethod paymentMethod, int bonusPoints) {
        this.institutionId = institutionId;
        this.items = items;
        this.paymentMethod = paymentMethod;
        this.bonusPoints = bonusPoints;
    }

    public Long getInstitutionId() {
        return institutionId;
    }

    public void setInstitutionId(Long institutionId) {
        this.institutionId = institutionId;
    }

    public List<OrderItemRequest> getItems() {
        return items;
    }

    public void setItems(List<OrderItemRequest> items) {
        this.items = items;
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
