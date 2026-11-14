package ua.readmania.dto.manager.analytics;

public class StockItemDto {

    private Long id;
    private String name;
    private long stock;

    public StockItemDto() {
    }

    public StockItemDto(Long id, String name, long stock) {
        this.id = id;
        this.name = name;
        this.stock = stock;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public long getStock() {
        return stock;
    }

    public void setStock(long stock) {
        this.stock = stock;
    }
}
