package ua.readmania.service;

import java.time.Instant;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ua.readmania.common.ConflictException;
import ua.readmania.common.NotFoundException;
import ua.readmania.domain.order.OrderRepository;
import ua.readmania.domain.order.OrderStatus;
import ua.readmania.domain.user.Role;
import ua.readmania.domain.user.UnblockRequest;
import ua.readmania.domain.user.UnblockRequestRepository;
import ua.readmania.domain.user.User;
import ua.readmania.domain.user.UserRepository;
import ua.readmania.domain.user.UserSpecifications;
import ua.readmania.domain.user.UserStatus;
import ua.readmania.dto.manager.user.BlockUserRequest;
import ua.readmania.dto.manager.user.ManagerUserDetailsResponse;
import ua.readmania.dto.manager.user.ManagerUnblockRequestResponse;
import ua.readmania.dto.manager.user.ManagerUserSummaryResponse;
import ua.readmania.dto.manager.user.UnblockUsersRequest;
import ua.readmania.service.notification.NotificationService;

@Service
@Transactional
public class ManagerUserService {

    private final UserRepository userRepository;
    private final OrderRepository orderRepository;
    private final UnblockRequestRepository unblockRequestRepository;
    private final NotificationService notificationService;

    public ManagerUserService(UserRepository userRepository,
                              OrderRepository orderRepository,
                              UnblockRequestRepository unblockRequestRepository,
                              NotificationService notificationService) {
        this.userRepository = userRepository;
        this.orderRepository = orderRepository;
        this.unblockRequestRepository = unblockRequestRepository;
        this.notificationService = notificationService;
    }

    @Transactional(readOnly = true)
    public Page<ManagerUserSummaryResponse> getUsers(String email, UserStatus status, Pageable pageable) {
        Page<User> users = userRepository.findAll(UserSpecifications.findUsers(email, status), pageable);
        return users.map(user -> new ManagerUserSummaryResponse(
                user.getId(),
                user.getEmail(),
                user.getStatus(),
                user.getCreatedAt(),
                user.getLastLoginAt()
        ));
    }

    @Transactional(readOnly = true)
    public ManagerUserDetailsResponse getUserDetails(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("Користувача з ID " + userId + " не знайдено"));

        Map<OrderStatus, Long> ordersByStatus = new EnumMap<>(OrderStatus.class);
        for (OrderStatus status : OrderStatus.values()) {
            ordersByStatus.put(status, 0L);
        }

        List<Object[]> statusCounts = orderRepository.countOrdersByStatusForUser(userId);
        for (Object[] row : statusCounts) {
            OrderStatus status = (OrderStatus) row[0];
            Long count = ((Number) row[1]).longValue();
            ordersByStatus.put(status, count);
        }

        long totalOrders = ordersByStatus.values().stream().mapToLong(Long::longValue).sum();

        return new ManagerUserDetailsResponse(
                user.getId(),
                user.getEmail(),
                user.getStatus(),
                user.getCreatedAt(),
                user.getLastLoginAt(),
                user.getBonusBalance(),
                totalOrders,
                ordersByStatus,
                user.getStatus() == UserStatus.BLOCKED ? user.getBlockedAt() : null,
                user.getStatus() == UserStatus.BLOCKED ? user.getBlockReason() : null
        );
    }

    public ManagerUserDetailsResponse blockUser(Long userId, BlockUserRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("Користувача з ID " + userId + " не знайдено"));

        if (user.getRole() != Role.USER) {
            throw new ConflictException("Неможливо заблокувати обліковий запис менеджера");
        }

        if (user.getStatus() == UserStatus.BLOCKED) {
            throw new ConflictException("Обліковий запис вже заблоковано");
        }

        user.setStatus(UserStatus.BLOCKED);
        user.setBlockReason(request.getReason());
        user.setBlockedAt(Instant.now());
        userRepository.save(user);

        notificationService.sendAccountBlocked(user);

        return getUserDetails(userId);
    }

    @Transactional(readOnly = true)
    public List<ManagerUnblockRequestResponse> getUnblockRequests() {
        List<UnblockRequest> requests = unblockRequestRepository.findAllByOrderByCreatedAtDesc();
        return requests.stream()
                .map(req -> new ManagerUnblockRequestResponse(
                        req.getId(),
                        req.getUser().getId(),
                        req.getUser().getEmail(),
                        req.getUser().getBlockedAt(),
                        req.getUser().getBlockReason(),
                        req.getReason(),
                        req.getCreatedAt()
                ))
                .toList();
    }

    public void unblockUsers(UnblockUsersRequest request) {
        for (Long userId : request.getUserIds()) {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new NotFoundException("Користувача з ID " + userId + " не знайдено"));

            if (user.getStatus() != UserStatus.BLOCKED) {
                throw new ConflictException("Користувач " + user.getEmail() + " не заблокований");
            }

            user.setStatus(UserStatus.ACTIVE);
            user.setBlockReason(null);
            user.setBlockedAt(null);
            user.setFailedLoginAttempts(0);
            user.setLockedUntil(null);
            userRepository.save(user);

            unblockRequestRepository.deleteByUserId(userId);
            notificationService.sendAccountUnblocked(user);
        }
    }
}
