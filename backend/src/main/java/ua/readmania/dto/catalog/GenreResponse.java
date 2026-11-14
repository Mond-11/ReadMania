package ua.readmania.dto.catalog;

import ua.readmania.domain.catalog.Genre;

public class GenreResponse {

    private Long id;
    private String name;
    private long booksCount;

    public GenreResponse() {
    }

    public GenreResponse(Long id, String name) {
        this(id, name, 0L);
    }

    public GenreResponse(Long id, String name, long booksCount) {
        this.id = id;
        this.name = name;
        this.booksCount = booksCount;
    }

    public static GenreResponse fromEntity(Genre genre) {
        return fromEntity(genre, 0L);
    }

    public static GenreResponse fromEntity(Genre genre, long booksCount) {
        if (genre == null) {
            return null;
        }
        return new GenreResponse(genre.getId(), genre.getName(), booksCount);
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

    public long getBooksCount() {
        return booksCount;
    }

    public void setBooksCount(long booksCount) {
        this.booksCount = booksCount;
    }
}
