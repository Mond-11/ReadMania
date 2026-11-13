package ua.readmania.dto.giftset;

import java.math.BigDecimal;
import java.util.List;
import ua.readmania.dto.catalog.GenreResponse;

public class GiftSetResponse {

    private List<GiftSetBookItem> items;
    private BigDecimal totalPrice;
    private BigDecimal budget;
    private BigDecimal remainingBudget;
    private List<GenreResponse> genres;

    public GiftSetResponse() {
    }

    public GiftSetResponse(List<GiftSetBookItem> items, BigDecimal totalPrice, BigDecimal budget, BigDecimal remainingBudget, List<GenreResponse> genres) {
        this.items = items;
        this.totalPrice = totalPrice;
        this.budget = budget;
        this.remainingBudget = remainingBudget;
        this.genres = genres;
    }

    public List<GiftSetBookItem> getItems() {
        return items;
    }

    public void setItems(List<GiftSetBookItem> items) {
        this.items = items;
    }

    public BigDecimal getTotalPrice() {
        return totalPrice;
    }

    public void setTotalPrice(BigDecimal totalPrice) {
        this.totalPrice = totalPrice;
    }

    public BigDecimal getBudget() {
        return budget;
    }

    public void setBudget(BigDecimal budget) {
        this.budget = budget;
    }

    public BigDecimal getRemainingBudget() {
        return remainingBudget;
    }

    public void setRemainingBudget(BigDecimal remainingBudget) {
        this.remainingBudget = remainingBudget;
    }

    public List<GenreResponse> getGenres() {
        return genres;
    }

    public void setGenres(List<GenreResponse> genres) {
        this.genres = genres;
    }
}
