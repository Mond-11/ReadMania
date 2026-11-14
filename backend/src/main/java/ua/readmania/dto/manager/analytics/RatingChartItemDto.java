package ua.readmania.dto.manager.analytics;

public class RatingChartItemDto {

    private Long id;
    private String name;
    private Double averageRating;
    private long ratingsCount;

    public RatingChartItemDto() {
    }

    public RatingChartItemDto(Long id, String name, Double averageRating, long ratingsCount) {
        this.id = id;
        this.name = name;
        this.averageRating = averageRating;
        this.ratingsCount = ratingsCount;
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

    public Double getAverageRating() {
        return averageRating;
    }

    public void setAverageRating(Double averageRating) {
        this.averageRating = averageRating;
    }

    public long getRatingsCount() {
        return ratingsCount;
    }

    public void setRatingsCount(long ratingsCount) {
        this.ratingsCount = ratingsCount;
    }
}
