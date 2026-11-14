package ua.readmania.service.gift;

import java.time.LocalDate;
import java.util.List;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ua.readmania.common.BusinessRuleException;
import ua.readmania.domain.gift.GiftLog;
import ua.readmania.domain.gift.GiftLogRepository;
import ua.readmania.domain.gift.GiftLogSpecifications;
import ua.readmania.dto.manager.gift.GiftLogEntryResponse;
import ua.readmania.dto.manager.gift.GiftLogPageResponse;

@Service
@Transactional(readOnly = true)
public class GiftLogService {

    private final GiftLogRepository giftLogRepository;

    public GiftLogService(GiftLogRepository giftLogRepository) {
        this.giftLogRepository = giftLogRepository;
    }

    public GiftLogPageResponse getGiftLog(Long institutionId, String institutionName, LocalDate dateFrom, LocalDate dateTo) {
        if (dateFrom != null && dateTo != null && dateTo.isBefore(dateFrom)) {
            throw new BusinessRuleException("Початкова дата не може бути пізнішою за кінцеву");
        }

        Specification<GiftLog> spec = GiftLogSpecifications.withFilters(institutionId, institutionName, dateFrom, dateTo);
        Sort sort = Sort.by(Sort.Direction.DESC, "deliveredDate", "id");

        List<GiftLog> giftLogs = giftLogRepository.findAll(spec, sort);

        List<GiftLogEntryResponse> items = giftLogs.stream()
                .map(GiftLogEntryResponse::fromEntity)
                .toList();

        long totalDonatedBooks = giftLogs.stream()
                .mapToLong(GiftLog::getQuantity)
                .sum();

        return new GiftLogPageResponse(items, totalDonatedBooks);
    }
}
