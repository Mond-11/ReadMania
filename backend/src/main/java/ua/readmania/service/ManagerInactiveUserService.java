package ua.readmania.service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ua.readmania.common.ConflictException;
import ua.readmania.domain.cart.CartItemRepository;
import ua.readmania.domain.loyalty.BonusTransactionRepository;
import ua.readmania.domain.loyalty.RatingRepository;
import ua.readmania.domain.order.OrderRepository;
import ua.readmania.domain.user.Role;
import ua.readmania.domain.user.UnblockRequestRepository;
import ua.readmania.domain.user.User;
import ua.readmania.domain.user.UserRepository;
import ua.readmania.domain.user.UserStatus;
import ua.readmania.dto.manager.user.DeleteInactiveUsersRequest;
import ua.readmania.dto.manager.user.InactiveUserResponse;

@Service
@Transactional
public class ManagerInactiveUserService {

    private final UserRepository userRepository;
    private final OrderRepository orderRepository;
    private final RatingRepository ratingRepository;
    private final CartItemRepository cartItemRepository;
    private final BonusTransactionRepository bonusTransactionRepository;
    private final UnblockRequestRepository unblockRequestRepository;

    @PersistenceContext
    private EntityManager entityManager;

    public ManagerInactiveUserService(UserRepository userRepository,
                                      OrderRepository orderRepository,
                                      RatingRepository ratingRepository,
                                      CartItemRepository cartItemRepository,
                                      BonusTransactionRepository bonusTransactionRepository,
                                      UnblockRequestRepository unblockRequestRepository) {
        this.userRepository = userRepository;
        this.orderRepository = orderRepository;
        this.ratingRepository = ratingRepository;
        this.cartItemRepository = cartItemRepository;
        this.bonusTransactionRepository = bonusTransactionRepository;
        this.unblockRequestRepository = unblockRequestRepository;
    }

    private Instant calculateCutoff() {
        return OffsetDateTime.now().minusYears(2).toInstant();
    }

    @Transactional(readOnly = true)
    public List<InactiveUserResponse> getInactiveUsers() {
        Instant cutoff = calculateCutoff();
        List<User> inactiveUsers = userRepository.findInactiveUsers(cutoff);

        List<InactiveUserResponse> response = new ArrayList<>();
        for (User user : inactiveUsers) {
            Instant refDate = calculateReferenceDate(user);
            response.add(new InactiveUserResponse(user.getId(), user.getEmail(), user.getStatus(), refDate));
        }
        return response;
    }

    public void deleteInactiveUsers(DeleteInactiveUsersRequest request) {
        if (request == null || request.getUserIds() == null || request.getUserIds().isEmpty()) {
            return;
        }

        Instant cutoff = calculateCutoff();
        List<Long> userIds = request.getUserIds();

        for (Long userId : userIds) {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new ConflictException("Користувача з ID " + userId + " не знайдено"));

            if (user.getRole() != Role.USER || !isEligibleForInactiveDeletion(user, cutoff)) {
                throw new ConflictException("Обліковий запис " + user.getEmail() + " (ID: " + userId + ") не відповідає критеріям неактивності понад 2 роки");
            }
        }

        // 1. Анонімізація замовлень
        orderRepository.anonymizeOrdersForUsers(userIds);

        // 2. Відв'язування оцінок від користувачів
        ratingRepository.anonymizeRatingsForUsers(userIds);

        entityManager.flush();
        entityManager.clear();

        // 3. Видалення зв'язаних даних та облікових записів
        for (Long userId : userIds) {
            cartItemRepository.deleteByUserId(userId);
            bonusTransactionRepository.deleteByUserId(userId);
            unblockRequestRepository.deleteByUserId(userId);
            userRepository.deleteById(userId);
        }
        userRepository.flush();
    }

    private boolean isEligibleForInactiveDeletion(User user, Instant cutoff) {
        if (user.getStatus() == UserStatus.ACTIVE) {
            Instant activityDate = user.getLastLoginAt() != null ? user.getLastLoginAt() : user.getCreatedAt();
            return activityDate != null && activityDate.isBefore(cutoff);
        } else if (user.getStatus() == UserStatus.BLOCKED) {
            Instant blockDate = user.getBlockedAt() != null ? user.getBlockedAt() : user.getCreatedAt();
            return blockDate != null && blockDate.isBefore(cutoff);
        }
        return false;
    }

    private Instant calculateReferenceDate(User user) {
        if (user.getStatus() == UserStatus.BLOCKED) {
            return user.getBlockedAt() != null ? user.getBlockedAt() : user.getCreatedAt();
        }
        return user.getLastLoginAt() != null ? user.getLastLoginAt() : user.getCreatedAt();
    }
}
