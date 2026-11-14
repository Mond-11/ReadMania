package ua.readmania.dto.manager.analytics;

import java.util.List;

public class DemandAnalyticsResponse {

    private List<ChartItemDto> byGenres;
    private List<ChartItemDto> byBooks;

    public DemandAnalyticsResponse() {
    }

    public DemandAnalyticsResponse(List<ChartItemDto> byGenres, List<ChartItemDto> byBooks) {
        this.byGenres = byGenres;
        this.byBooks = byBooks;
    }

    public List<ChartItemDto> getByGenres() {
        return byGenres;
    }

    public void setByGenres(List<ChartItemDto> byGenres) {
        this.byGenres = byGenres;
    }

    public List<ChartItemDto> getByBooks() {
        return byBooks;
    }

    public void setByBooks(List<ChartItemDto> byBooks) {
        this.byBooks = byBooks;
    }
}
