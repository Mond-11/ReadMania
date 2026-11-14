package ua.readmania.dto.manager.analytics;

import java.util.List;

public class StockAnalyticsResponse {

    private List<StockItemDto> byGenres;
    private List<StockItemDto> byBooks;

    public StockAnalyticsResponse() {
    }

    public StockAnalyticsResponse(List<StockItemDto> byGenres, List<StockItemDto> byBooks) {
        this.byGenres = byGenres;
        this.byBooks = byBooks;
    }

    public List<StockItemDto> getByGenres() {
        return byGenres;
    }

    public void setByGenres(List<StockItemDto> byGenres) {
        this.byGenres = byGenres;
    }

    public List<StockItemDto> getByBooks() {
        return byBooks;
    }

    public void setByBooks(List<StockItemDto> byBooks) {
        this.byBooks = byBooks;
    }
}
