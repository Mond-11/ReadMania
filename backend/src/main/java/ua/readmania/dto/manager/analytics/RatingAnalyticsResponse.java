package ua.readmania.dto.manager.analytics;

import java.util.List;

public class RatingAnalyticsResponse {

    private List<RatingChartItemDto> byGenres;
    private List<RatingChartItemDto> byBooks;

    public RatingAnalyticsResponse() {
    }

    public RatingAnalyticsResponse(List<RatingChartItemDto> byGenres, List<RatingChartItemDto> byBooks) {
        this.byGenres = byGenres;
        this.byBooks = byBooks;
    }

    public List<RatingChartItemDto> getByGenres() {
        return byGenres;
    }

    public void setByGenres(List<RatingChartItemDto> byGenres) {
        this.byGenres = byGenres;
    }

    public List<RatingChartItemDto> getByBooks() {
        return byBooks;
    }

    public void setByBooks(List<RatingChartItemDto> byBooks) {
        this.byBooks = byBooks;
    }
}
