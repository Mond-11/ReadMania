package ua.readmania.controller;

import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ua.readmania.domain.institution.InstitutionType;
import ua.readmania.dto.institution.InstitutionResponse;
import ua.readmania.service.institution.InstitutionService;

@RestController
@RequestMapping("/api/institutions")
public class InstitutionController {

    private final InstitutionService institutionService;

    public InstitutionController(InstitutionService institutionService) {
        this.institutionService = institutionService;
    }

    @GetMapping
    public ResponseEntity<List<InstitutionResponse>> getInstitutions(
            @RequestParam(name = "type", required = false) InstitutionType type,
            @RequestParam(name = "query", required = false) String query) {
        List<InstitutionResponse> response = institutionService.getInstitutions(type, query);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<InstitutionResponse> getInstitutionById(@PathVariable("id") Long id) {
        InstitutionResponse response = institutionService.getInstitutionById(id);
        return ResponseEntity.ok(response);
    }
}
