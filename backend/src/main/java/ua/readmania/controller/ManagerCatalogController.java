package ua.readmania.controller;

import jakarta.validation.Valid;
import java.io.IOException;
import java.io.InputStream;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import ua.readmania.common.BusinessRuleException;
import ua.readmania.dto.catalog.BookDetailsResponse;
import ua.readmania.dto.catalog.CreateBookRequest;
import ua.readmania.dto.catalog.CsvImportResponse;
import ua.readmania.dto.catalog.GenreRequest;
import ua.readmania.dto.catalog.GenreResponse;
import ua.readmania.dto.catalog.UpdateBookRequest;
import ua.readmania.service.ManagerCatalogService;

@RestController
@RequestMapping("/api/manager")
public class ManagerCatalogController {

    private final ManagerCatalogService managerCatalogService;

    public ManagerCatalogController(ManagerCatalogService managerCatalogService) {
        this.managerCatalogService = managerCatalogService;
    }

    // =========================================================================
    // Books Management (M1.1, M1.2, M1.3)
    // =========================================================================

    @PostMapping("/books")
    public ResponseEntity<BookDetailsResponse> createBook(@Valid @RequestBody CreateBookRequest request) {
        BookDetailsResponse response = managerCatalogService.createBook(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/books/{id}")
    public BookDetailsResponse updateBook(
            @PathVariable("id") Long id,
            @Valid @RequestBody UpdateBookRequest request
    ) {
        return managerCatalogService.updateBook(id, request);
    }

    @DeleteMapping("/books/{id}")
    public ResponseEntity<Void> deleteBook(@PathVariable("id") Long id) {
        managerCatalogService.deleteBook(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping(value = "/books/{id}/cover", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Void> uploadBookCover(
            @PathVariable("id") Long id,
            @RequestParam("file") MultipartFile file
    ) {
        if (file.isEmpty()) {
            throw new BusinessRuleException("Файл обкладинки не може бути порожнім");
        }
        try {
            managerCatalogService.uploadCover(id, file.getBytes(), file.getContentType());
            return ResponseEntity.ok().build();
        } catch (IOException e) {
            throw new BusinessRuleException("Помилка читання файлу обкладинки: " + e.getMessage());
        }
    }

    // =========================================================================
    // Genres Management (M1)
    // =========================================================================

    @PostMapping("/genres")
    public ResponseEntity<GenreResponse> createGenre(@Valid @RequestBody GenreRequest request) {
        GenreResponse response = managerCatalogService.createGenre(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/genres/{id}")
    public GenreResponse updateGenre(
            @PathVariable("id") Long id,
            @Valid @RequestBody GenreRequest request
    ) {
        return managerCatalogService.updateGenre(id, request);
    }

    @DeleteMapping("/genres/{id}")
    public ResponseEntity<Void> deleteGenre(@PathVariable("id") Long id) {
        managerCatalogService.deleteGenre(id);
        return ResponseEntity.noContent().build();
    }

    // =========================================================================
    // CSV Import (M2)
    // =========================================================================

    @PostMapping(value = "/books/import-csv", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public CsvImportResponse importBooksFromCsv(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            throw new BusinessRuleException("CSV-файл не може бути порожнім");
        }
        try (InputStream is = file.getInputStream()) {
            return managerCatalogService.importFromCsv(is);
        } catch (IOException e) {
            throw new BusinessRuleException("Помилка читання CSV-файлу: " + e.getMessage());
        }
    }
}
