package ua.readmania.service.loyalty;

import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ua.readmania.common.NotFoundException;
import ua.readmania.domain.loyalty.BonusTransaction;
import ua.readmania.domain.loyalty.BonusTransactionRepository;
import ua.readmania.domain.user.User;
import ua.readmania.domain.user.UserRepository;
import ua.readmania.dto.loyalty.BonusTransactionResponse;
import ua.readmania.dto.loyalty.LoyaltyBalanceResponse;

@Service
public class LoyaltyService {

    private final UserRepository userRepository;
    private final BonusTransactionRepository bonusTransactionRepository;

    public LoyaltyService(UserRepository userRepository,
                          BonusTransactionRepository bonusTransactionRepository) {
        this.userRepository = userRepository;
        this.bonusTransactionRepository = bonusTransactionRepository;
    }

    @Transactional(readOnly = true)
    public LoyaltyBalanceResponse getLoyaltyBalance(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("Користувача не знайдено"));

        List<BonusTransaction> transactions = bonusTransactionRepository.findByUserIdOrderByCreatedAtDesc(userId);
        List<BonusTransactionResponse> txResponses = transactions.stream()
                .map(BonusTransactionResponse::fromEntity)
                .toList();

        return new LoyaltyBalanceResponse(user.getBonusBalance(), txResponses);
    }
}
