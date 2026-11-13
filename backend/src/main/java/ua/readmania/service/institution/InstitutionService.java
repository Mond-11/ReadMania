package ua.readmania.service.institution;

import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ua.readmania.common.NotFoundException;
import ua.readmania.domain.institution.Institution;
import ua.readmania.domain.institution.InstitutionRepository;
import ua.readmania.domain.institution.InstitutionType;
import ua.readmania.dto.institution.InstitutionResponse;

@Service
public class InstitutionService {

    private final InstitutionRepository institutionRepository;

    public InstitutionService(InstitutionRepository institutionRepository) {
        this.institutionRepository = institutionRepository;
    }

    @Transactional(readOnly = true)
    public List<InstitutionResponse> getInstitutions(InstitutionType type, String query) {
        List<Institution> institutions;
        if (query != null && !query.isBlank()) {
            institutions = institutionRepository.findByNameContainingIgnoreCase(query.trim());
            if (type != null) {
                institutions = institutions.stream().filter(i -> i.getType() == type).toList();
            }
        } else if (type != null) {
            institutions = institutionRepository.findByType(type);
        } else {
            institutions = institutionRepository.findAll();
        }

        return institutions.stream()
                .map(InstitutionResponse::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public InstitutionResponse getInstitutionById(Long id) {
        Institution institution = institutionRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Заклад з ідентифікатором " + id + " не знайдено"));
        return InstitutionResponse.fromEntity(institution);
    }
}
