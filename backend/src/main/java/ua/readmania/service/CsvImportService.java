package ua.readmania.service;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ua.readmania.domain.catalog.Author;
import ua.readmania.domain.catalog.AuthorRepository;
import ua.readmania.domain.catalog.Book;
import ua.readmania.domain.catalog.BookCover;
import ua.readmania.domain.catalog.BookCoverRepository;
import ua.readmania.domain.catalog.BookRepository;
import ua.readmania.domain.catalog.Genre;
import ua.readmania.domain.catalog.GenreRepository;
import ua.readmania.dto.catalog.CsvImportResponse;
import ua.readmania.dto.catalog.CsvRejectedRow;

@Service
public class CsvImportService {

    private static final Logger log = LoggerFactory.getLogger(CsvImportService.class);

    private final BookRepository bookRepository;
    private final GenreRepository genreRepository;
    private final AuthorRepository authorRepository;
    private final BookCoverRepository bookCoverRepository;
    private final HttpClient httpClient;

    public CsvImportService(BookRepository bookRepository,
                            GenreRepository genreRepository,
                            AuthorRepository authorRepository,
                            BookCoverRepository bookCoverRepository) {
        this.bookRepository = bookRepository;
        this.genreRepository = genreRepository;
        this.authorRepository = authorRepository;
        this.bookCoverRepository = bookCoverRepository;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(4))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
    }

    @Transactional
    public CsvImportResponse importCsv(InputStream inputStream) {
        List<CsvRejectedRow> rejectedRows = new ArrayList<>();
        int totalRows = 0;
        int addedCount = 0;
        Set<String> seenIsbnsInFile = new HashSet<>();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            String headerLine = reader.readLine();
            if (headerLine == null || headerLine.trim().isEmpty()) {
                return new CsvImportResponse(0, 0, 0, List.of());
            }

            char delimiter = detectDelimiter(headerLine);
            List<String> headers = parseCsvLine(headerLine, delimiter).stream()
                    .map(h -> h.trim().toLowerCase(Locale.ROOT))
                    .toList();

            int titleIdx = findHeaderIndex(headers, "title", "назва", "назва книги", "book_title");
            int isbnIdx = findHeaderIndex(headers, "isbn", "ісбн");
            int priceIdx = findHeaderIndex(headers, "price", "ціна", "вартість");
            int stockIdx = findHeaderIndex(headers, "stock", "кількість", "залишок", "наявність");
            int descriptionIdx = findHeaderIndex(headers, "description", "опис");
            int genreIdx = findHeaderIndex(headers, "genre", "жанр", "категорія");
            int authorsIdx = findHeaderIndex(headers, "authors", "author", "автори", "автор");
            int coverUrlIdx = findHeaderIndex(headers, "cover", "coverurl", "cover_url", "image", "imageurl", "image_url", "обкладинка");

            if (titleIdx == -1 || isbnIdx == -1 || priceIdx == -1) {
                rejectedRows.add(new CsvRejectedRow(1, "", "", "Файл не містить обов'язкових колонок: title, isbn, price"));
                return new CsvImportResponse(0, 0, 1, rejectedRows);
            }

            String line;
            int rowNumber = 1; // 1 is header
            while ((line = reader.readLine()) != null) {
                rowNumber++;
                if (line.trim().isEmpty()) {
                    continue;
                }
                totalRows++;

                List<String> values = parseCsvLine(line, delimiter);

                String title = getValue(values, titleIdx);
                String isbn = getValue(values, isbnIdx);
                String priceStr = getValue(values, priceIdx);
                String stockStr = getValue(values, stockIdx);
                String description = getValue(values, descriptionIdx);
                String genreName = getValue(values, genreIdx);
                String authorsStr = getValue(values, authorsIdx);
                String coverUrl = getValue(values, coverUrlIdx);

                if (title == null || title.isBlank()) {
                    rejectedRows.add(new CsvRejectedRow(rowNumber, isbn, title, "Назва книги не може бути порожньою"));
                    continue;
                }

                if (isbn == null || isbn.isBlank()) {
                    rejectedRows.add(new CsvRejectedRow(rowNumber, isbn, title, "ISBN не може бути порожнім"));
                    continue;
                }

                String cleanIsbn = isbn.trim();
                if (seenIsbnsInFile.contains(cleanIsbn)) {
                    rejectedRows.add(new CsvRejectedRow(rowNumber, cleanIsbn, title, "Дублікат ISBN у файлі: " + cleanIsbn));
                    continue;
                }
                seenIsbnsInFile.add(cleanIsbn);

                if (bookRepository.existsActiveByIsbn(cleanIsbn)) {
                    rejectedRows.add(new CsvRejectedRow(rowNumber, cleanIsbn, title, "Книга з ISBN " + cleanIsbn + " вже існує в базі даних"));
                    continue;
                }

                BigDecimal price;
                try {
                    price = new BigDecimal(priceStr != null ? priceStr.trim().replace(',', '.') : "0");
                    if (price.compareTo(BigDecimal.ZERO) < 0) {
                        rejectedRows.add(new CsvRejectedRow(rowNumber, cleanIsbn, title, "Ціна не може бути від'ємною: " + priceStr));
                        continue;
                    }
                } catch (Exception e) {
                    rejectedRows.add(new CsvRejectedRow(rowNumber, cleanIsbn, title, "Некоректний формат ціни: " + priceStr));
                    continue;
                }

                int stock = 0;
                if (stockStr != null && !stockStr.isBlank()) {
                    try {
                        stock = Integer.parseInt(stockStr.trim());
                        if (stock < 0) {
                            rejectedRows.add(new CsvRejectedRow(rowNumber, cleanIsbn, title, "Кількість на складі не може бути від'ємною: " + stockStr));
                            continue;
                        }
                    } catch (Exception e) {
                        rejectedRows.add(new CsvRejectedRow(rowNumber, cleanIsbn, title, "Некоректний формат залишку на складі: " + stockStr));
                        continue;
                    }
                }

                // Resolve Genre
                Genre genre;
                if (genreName != null && !genreName.isBlank()) {
                    String cleanGenre = genreName.trim();
                    genre = genreRepository.findByNameIgnoreCase(cleanGenre)
                            .orElseGet(() -> genreRepository.save(new Genre(cleanGenre)));
                } else {
                    genre = genreRepository.findByNameIgnoreCase("Загальне")
                            .orElseGet(() -> genreRepository.save(new Genre("Загальне")));
                }

                // Resolve Authors
                Set<Author> authors = new LinkedHashSet<>();
                if (authorsStr != null && !authorsStr.isBlank()) {
                    // Split authors by semicolon, comma (if semicolon not present) or slash
                    String[] parts = authorsStr.contains(";") ? authorsStr.split(";") : authorsStr.split(",");
                    for (String authorPart : parts) {
                        String cleanAuthor = authorPart.trim();
                        if (!cleanAuthor.isBlank()) {
                            Author author = authorRepository.findByNameIgnoreCase(cleanAuthor)
                                    .orElseGet(() -> authorRepository.save(new Author(cleanAuthor)));
                            authors.add(author);
                        }
                    }
                }
                if (authors.isEmpty()) {
                    Author defaultAuthor = authorRepository.findByNameIgnoreCase("Невідомий автор")
                            .orElseGet(() -> authorRepository.save(new Author("Невідомий автор")));
                    authors.add(defaultAuthor);
                }

                // Create and save Book
                Book book = new Book(title.trim(), cleanIsbn, price, stock, genre, authors);
                if (description != null && !description.isBlank()) {
                    book.setDescription(description.trim());
                }
                book = bookRepository.save(book);

                // Fetch and save cover if URL present
                if (coverUrl != null && !coverUrl.isBlank() && (coverUrl.startsWith("http://") || coverUrl.startsWith("https://"))) {
                    tryDownloadAndSaveCover(book, coverUrl.trim());
                }

                addedCount++;
            }
        } catch (Exception ex) {
            log.error("Помилка під час обробки CSV-файлу: {}", ex.getMessage(), ex);
            rejectedRows.add(new CsvRejectedRow(0, "", "", "Помилка читання файлу: " + ex.getMessage()));
        }

        return new CsvImportResponse(totalRows, addedCount, rejectedRows.size(), rejectedRows);
    }

    private void tryDownloadAndSaveCover(Book book, String url) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(4))
                    .GET()
                    .build();

            HttpResponse<byte[]> response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());
            if (response.statusCode() == 200 && response.body() != null && response.body().length > 0) {
                String contentType = response.headers().firstValue("Content-Type").orElse("image/jpeg");
                BookCover cover = new BookCover(book, contentType, response.body());
                bookCoverRepository.save(cover);
            }
        } catch (Exception ex) {
            log.warn("Не вдалося завантажити обкладинку для книги '{}' за URL {}: {}", book.getTitle(), url, ex.getMessage());
        }
    }

    private char detectDelimiter(String headerLine) {
        if (headerLine.contains(";")) {
            return ';';
        }
        if (headerLine.contains("\t")) {
            return '\t';
        }
        return ',';
    }

    private int findHeaderIndex(List<String> headers, String... possibleNames) {
        for (int i = 0; i < headers.size(); i++) {
            String h = headers.get(i);
            for (String name : possibleNames) {
                if (h.equalsIgnoreCase(name) || h.replaceAll("[_\\-\\s]", "").equalsIgnoreCase(name.replaceAll("[_\\-\\s]", ""))) {
                    return i;
                }
            }
        }
        return -1;
    }

    private String getValue(List<String> values, int index) {
        if (index >= 0 && index < values.size()) {
            return values.get(index);
        }
        return null;
    }

    public static List<String> parseCsvLine(String line, char delimiter) {
        List<String> tokens = new ArrayList<>();
        StringBuilder sb = new StringBuilder();
        boolean inQuotes = false;

        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '\"') {
                if (inQuotes && i + 1 < line.length() && line.charAt(i + 1) == '\"') {
                    sb.append('\"');
                    i++;
                } else {
                    inQuotes = !inQuotes;
                }
            } else if (c == delimiter && !inQuotes) {
                tokens.add(sb.toString());
                sb.setLength(0);
            } else {
                sb.append(c);
            }
        }
        tokens.add(sb.toString());
        return tokens;
    }
}
