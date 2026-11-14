package ua.readmania.dto.manager.analytics;

public class ChartItemDto {

    private Long id;
    private String name;
    private long orderedQuantity;

    public ChartItemDto() {
    }

    public ChartItemDto(Long id, String name, long orderedQuantity) {
        this.id = id;
        this.name = name;
        this.orderedQuantity = orderedQuantity;
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

    public long getOrderedQuantity() {
        return orderedQuantity;
    }

    public void setOrderedQuantity(long orderedQuantity) {
        this.orderedQuantity = orderedQuantity;
    }
}
