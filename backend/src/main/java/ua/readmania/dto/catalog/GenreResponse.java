package ua.readmania.dto.catalog;

import ua.readmania.domain.catalog.Genre;

public class GenreResponse {

    private Long id;
    private String name;

    public GenreResponse() {
    }

    public GenreResponse(Long id, String name) {
        this.id = id;
        this.name = name;
    }

    public static GenreResponse fromEntity(Genre genre) {
        if (genre == null) {
            return null;
        }
        return new GenreResponse(genre.getId(), genre.getName());
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
}
