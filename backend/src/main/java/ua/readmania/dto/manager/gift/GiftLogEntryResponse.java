package ua.readmania.dto.manager.gift;

import java.time.LocalDate;
import ua.readmania.domain.gift.GiftLog;

public class GiftLogEntryResponse {

    private Long id;
    private Long orderId;
    private String institutionName;
    private String bookTitle;
    private int quantity;
    private LocalDate deliveredAt;

    public GiftLogEntryResponse() {
    }

    public GiftLogEntryResponse(Long id, Long orderId, String institutionName, String bookTitle, int quantity, LocalDate deliveredAt) {
        this.id = id;
        this.orderId = orderId;
        this.institutionName = institutionName;
        this.bookTitle = bookTitle;
        this.quantity = quantity;
        this.deliveredAt = deliveredAt;
    }

    public static GiftLogEntryResponse fromEntity(GiftLog giftLog) {
        if (giftLog == null) {
            return null;
        }
        Long orderId = giftLog.getOrder() != null ? giftLog.getOrder().getId() : null;
        return new GiftLogEntryResponse(
                giftLog.getId(),
                orderId,
                giftLog.getInstitutionName(),
                giftLog.getBookTitle(),
                giftLog.getQuantity(),
                giftLog.getDeliveredDate()
        );
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getOrderId() {
        return orderId;
    }

    public void setOrderId(Long orderId) {
        this.orderId = orderId;
    }

    public String getInstitutionName() {
        return institutionName;
    }

    public void setInstitutionName(String institutionName) {
        this.institutionName = institutionName;
    }

    public String getBookTitle() {
        return bookTitle;
    }

    public void setBookTitle(String bookTitle) {
        this.bookTitle = bookTitle;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public LocalDate getDeliveredAt() {
        return deliveredAt;
    }

    public void setDeliveredAt(LocalDate deliveredAt) {
        this.deliveredAt = deliveredAt;
    }
}
