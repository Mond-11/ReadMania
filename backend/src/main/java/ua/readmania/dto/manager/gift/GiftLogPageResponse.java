package ua.readmania.dto.manager.gift;

import java.util.List;

public class GiftLogPageResponse {

    private List<GiftLogEntryResponse> items;
    private long totalDonatedBooks;

    public GiftLogPageResponse() {
    }

    public GiftLogPageResponse(List<GiftLogEntryResponse> items, long totalDonatedBooks) {
        this.items = items;
        this.totalDonatedBooks = totalDonatedBooks;
    }

    public List<GiftLogEntryResponse> getItems() {
        return items;
    }

    public void setItems(List<GiftLogEntryResponse> items) {
        this.items = items;
    }

    public long getTotalDonatedBooks() {
        return totalDonatedBooks;
    }

    public void setTotalDonatedBooks(long totalDonatedBooks) {
        this.totalDonatedBooks = totalDonatedBooks;
    }
}
