package ua.readmania.controller;

import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ua.readmania.domain.institution.InstitutionType;
import ua.readmania.dto.institution.DeleteInstitutionsRequest;
import ua.readmania.dto.institution.InstitutionDetailsResponse;
import ua.readmania.dto.institution.InstitutionUpsertRequest;
import ua.readmania.service.institution.InstitutionService;

@RestController
@RequestMapping("/api/manager/institutions")
public class ManagerInstitutionController {

    private final InstitutionService institutionService;

    public ManagerInstitutionController(InstitutionService institutionService) {
        this.institutionService = institutionService;
    }

    @GetMapping
    public ResponseEntity<List<InstitutionDetailsResponse>> getInstitutions(
            @RequestParam(name = "query", required = false) String query,
            @RequestParam(name = "type", required = false) InstitutionType type) {
        List<InstitutionDetailsResponse> response = institutionService.getManagerInstitutions(query, type);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<InstitutionDetailsResponse> getInstitutionById(@PathVariable("id") Long id) {
        InstitutionDetailsResponse response = institutionService.getManagerInstitutionById(id);
        return ResponseEntity.ok(response);
    }

    @PostMapping
    public ResponseEntity<InstitutionDetailsResponse> createInstitution(
            @Valid @RequestBody InstitutionUpsertRequest request) {
        InstitutionDetailsResponse response = institutionService.createInstitution(request);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<InstitutionDetailsResponse> updateInstitution(
            @PathVariable("id") Long id,
            @Valid @RequestBody InstitutionUpsertRequest request) {
        InstitutionDetailsResponse response = institutionService.updateInstitution(id, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping
    public ResponseEntity<Map<String, String>> deleteInstitutions(
            @Valid @RequestBody DeleteInstitutionsRequest request) {
        institutionService.deleteInstitutions(request);
        return ResponseEntity.ok(Map.of("message", "Заклади успішно видалено"));
    }
}
