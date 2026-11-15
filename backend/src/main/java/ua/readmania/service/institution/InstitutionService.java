package ua.readmania.service.institution;

import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ua.readmania.common.ConflictException;
import ua.readmania.common.NotFoundException;
import ua.readmania.domain.gift.GiftLogRepository;
import ua.readmania.domain.institution.Institution;
import ua.readmania.domain.institution.InstitutionRepository;
import ua.readmania.domain.institution.InstitutionType;
import ua.readmania.domain.order.Order;
import ua.readmania.domain.order.OrderRepository;
import ua.readmania.domain.order.OrderStatus;
import ua.readmania.dto.institution.DeleteInstitutionsRequest;
import ua.readmania.dto.institution.InstitutionDetailsResponse;
import ua.readmania.dto.institution.InstitutionResponse;
import ua.readmania.dto.institution.InstitutionUpsertRequest;

@Service
@Transactional
public class InstitutionService {

    private final InstitutionRepository institutionRepository;
    private final OrderRepository orderRepository;
    private final GiftLogRepository giftLogRepository;

    public InstitutionService(InstitutionRepository institutionRepository,
                              OrderRepository orderRepository,
                              GiftLogRepository giftLogRepository) {
        this.institutionRepository = institutionRepository;
        this.orderRepository = orderRepository;
        this.giftLogRepository = giftLogRepository;
    }

    @Transactional(readOnly = true)
    public List<InstitutionResponse> getInstitutions(InstitutionType type, String query) {
        List<Institution> institutions = institutionRepository.searchInstitutions(type, query);
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

    @Transactional(readOnly = true)
    public List<InstitutionDetailsResponse> getManagerInstitutions(String query, InstitutionType type) {
        List<Institution> institutions = institutionRepository.searchInstitutions(type, query);
        List<InstitutionDetailsResponse> responses = new ArrayList<>();
        for (Institution institution : institutions) {
            long count = orderRepository.countByInstitutionId(institution.getId());
            boolean canDelete = !orderRepository.existsByInstitutionIdAndStatusIn(
                    institution.getId(),
                    List.of(OrderStatus.AWAITING_PAYMENT, OrderStatus.PAID)
            );
            responses.add(InstitutionDetailsResponse.of(institution, count, canDelete));
        }
        return responses;
    }

    @Transactional(readOnly = true)
    public InstitutionDetailsResponse getManagerInstitutionById(Long id) {
        Institution institution = institutionRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Заклад з ідентифікатором " + id + " не знайдено"));
        long count = orderRepository.countByInstitutionId(institution.getId());
        boolean canDelete = !orderRepository.existsByInstitutionIdAndStatusIn(
                institution.getId(),
                List.of(OrderStatus.AWAITING_PAYMENT, OrderStatus.PAID)
        );
        return InstitutionDetailsResponse.of(institution, count, canDelete);
    }

    public InstitutionDetailsResponse createInstitution(InstitutionUpsertRequest request) {
        String cleanName = request.getName().trim();
        if (institutionRepository.existsByNameIgnoreCase(cleanName)) {
            throw new ConflictException("Заклад з назвою '" + cleanName + "' вже існує");
        }

        Institution institution = new Institution();
        institution.setName(cleanName);
        institution.setType(request.getType());
        institution.setAddress(request.getAddress().trim());
        institution.setContactPerson(request.getContactPerson().trim());

        institution = institutionRepository.save(institution);
        return InstitutionDetailsResponse.of(institution, 0, true);
    }

    public InstitutionDetailsResponse updateInstitution(Long id, InstitutionUpsertRequest request) {
        Institution institution = institutionRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Заклад з ідентифікатором " + id + " не знайдено"));

        String cleanName = request.getName().trim();
        institutionRepository.findByNameIgnoreCase(cleanName)
                .filter(existing -> !existing.getId().equals(id))
                .ifPresent(existing -> {
                    throw new ConflictException("Заклад з назвою '" + cleanName + "' вже існує");
                });

        institution.setName(cleanName);
        institution.setType(request.getType());
        institution.setAddress(request.getAddress().trim());
        institution.setContactPerson(request.getContactPerson().trim());

        institution = institutionRepository.save(institution);

        long count = orderRepository.countByInstitutionId(institution.getId());
        boolean canDelete = !orderRepository.existsByInstitutionIdAndStatusIn(
                institution.getId(),
                List.of(OrderStatus.AWAITING_PAYMENT, OrderStatus.PAID)
        );
        return InstitutionDetailsResponse.of(institution, count, canDelete);
    }

    public void deleteInstitutions(DeleteInstitutionsRequest request) {
        if (request == null || request.getInstitutionIds() == null || request.getInstitutionIds().isEmpty()) {
            return;
        }

        List<Long> institutionIds = request.getInstitutionIds();
        List<String> blockedInstitutions = new ArrayList<>();

        for (Long id : institutionIds) {
            Institution institution = institutionRepository.findById(id)
                    .orElseThrow(() -> new NotFoundException("Заклад з ідентифікатором " + id + " не знайдено"));

            boolean hasActiveOrders = orderRepository.existsByInstitutionIdAndStatusIn(
                    id,
                    List.of(OrderStatus.AWAITING_PAYMENT, OrderStatus.PAID)
            );

            if (hasActiveOrders) {
                blockedInstitutions.add(institution.getName() + " (ID: " + id + ")");
            }
        }

        if (!blockedInstitutions.isEmpty()) {
            throw new ConflictException("Неможливо видалити заклади, оскільки для них є активні замовлення в обробці: "
                    + String.join(", ", blockedInstitutions));
        }

        // Відв'язуємо записи gift_log від закладів та замовлень перед їх видаленням
        giftLogRepository.detachInstitutions(institutionIds);

        // Видалення завершених та скасованих замовлень у дар для цих закладів
        List<Order> finishedOrders = orderRepository.findByInstitutionIdInAndStatusIn(
                institutionIds,
                List.of(OrderStatus.DELIVERED, OrderStatus.CANCELLED)
        );
        if (!finishedOrders.isEmpty()) {
            List<Long> orderIds = finishedOrders.stream().map(Order::getId).toList();
            giftLogRepository.detachOrders(orderIds);
            orderRepository.deleteAll(finishedOrders);
        }

        // Видалення закладів
        institutionRepository.deleteAllById(institutionIds);
    }
}
