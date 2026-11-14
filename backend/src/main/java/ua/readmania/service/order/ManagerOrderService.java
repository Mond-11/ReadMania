package ua.readmania.service.order;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ua.readmania.common.BusinessRuleException;
import ua.readmania.common.ConflictException;
import ua.readmania.common.NotFoundException;
import ua.readmania.domain.catalog.Book;
import ua.readmania.domain.catalog.BookRepository;
import ua.readmania.domain.challenge.Challenge;
import ua.readmania.domain.challenge.ChallengeCompletion;
import ua.readmania.domain.challenge.ChallengeCompletionRepository;
import ua.readmania.domain.challenge.ChallengeRepository;
import ua.readmania.domain.challenge.ChallengeStatus;
import ua.readmania.domain.gift.GiftLog;
import ua.readmania.domain.gift.GiftLogRepository;
import ua.readmania.domain.loyalty.BonusTransaction;
import ua.readmania.domain.loyalty.BonusTransactionRepository;
import ua.readmania.domain.loyalty.BonusTransactionType;
import ua.readmania.domain.order.Order;
import ua.readmania.domain.order.OrderItem;
import ua.readmania.domain.order.OrderItemRepository;
import ua.readmania.domain.order.OrderRepository;
import ua.readmania.domain.order.OrderStatus;
import ua.readmania.domain.order.OrderStatusHistory;
import ua.readmania.domain.order.OrderStatusHistoryRepository;
import ua.readmania.domain.order.OrderType;
import ua.readmania.domain.order.PaymentMethod;
import ua.readmania.domain.user.User;
import ua.readmania.domain.user.UserRepository;
import ua.readmania.dto.order.OrderDetailsResponse;
import ua.readmania.dto.order.OrderStatusHistoryResponse;
import ua.readmania.dto.order.OrderSummaryResponse;
import ua.readmania.dto.order.UpdateOrderStatusRequest;

@Service
public class ManagerOrderService {

    private static final Logger log = LoggerFactory.getLogger(ManagerOrderService.class);

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final OrderStatusHistoryRepository orderStatusHistoryRepository;
    private final BookRepository bookRepository;
    private final UserRepository userRepository;
    private final BonusTransactionRepository bonusTransactionRepository;
    private final ChallengeRepository challengeRepository;
    private final ChallengeCompletionRepository challengeCompletionRepository;
    private final GiftLogRepository giftLogRepository;

    public ManagerOrderService(OrderRepository orderRepository,
                               OrderItemRepository orderItemRepository,
                               OrderStatusHistoryRepository orderStatusHistoryRepository,
                               BookRepository bookRepository,
                               UserRepository userRepository,
                               BonusTransactionRepository bonusTransactionRepository,
                               ChallengeRepository challengeRepository,
                               ChallengeCompletionRepository challengeCompletionRepository,
                               GiftLogRepository giftLogRepository) {
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.orderStatusHistoryRepository = orderStatusHistoryRepository;
        this.bookRepository = bookRepository;
        this.userRepository = userRepository;
        this.bonusTransactionRepository = bonusTransactionRepository;
        this.challengeRepository = challengeRepository;
        this.challengeCompletionRepository = challengeCompletionRepository;
        this.giftLogRepository = giftLogRepository;
    }

    @Transactional(readOnly = true)
    public Page<OrderSummaryResponse> getOrders(OrderStatus status, Boolean unpaidOverdue, Boolean archived, Pageable pageable) {
        if (Boolean.TRUE.equals(unpaidOverdue)) {
            Instant before = Instant.now().minus(7, ChronoUnit.DAYS);
            return orderRepository.findUnpaidOrdersOlderThanPaged(before, pageable)
                    .map(OrderSummaryResponse::fromEntity);
        }

        if (Boolean.TRUE.equals(archived)) {
            Instant before = Instant.now().minus(3 * 365, ChronoUnit.DAYS);
            return orderRepository.findArchivedOrdersOlderThanPaged(before, pageable)
                    .map(OrderSummaryResponse::fromEntity);
        }

        if (status != null) {
            return orderRepository.findByStatusOrderByCreatedAtDesc(status, pageable)
                    .map(OrderSummaryResponse::fromEntity);
        }

        return orderRepository.findAllByOrderByCreatedAtDesc(pageable)
                .map(OrderSummaryResponse::fromEntity);
    }

    @Transactional(readOnly = true)
    public OrderDetailsResponse getOrderById(Long orderId) {
        Order order = orderRepository.findWithDetailsById(orderId)
                .orElseThrow(() -> new NotFoundException("Замовлення з ID " + orderId + " не знайдено"));

        List<OrderStatusHistoryResponse> history = orderStatusHistoryRepository.findByOrderIdOrderByChangedAtAsc(orderId)
                .stream()
                .map(OrderStatusHistoryResponse::fromEntity)
                .toList();

        return OrderDetailsResponse.fromEntity(order, history);
    }

    @Transactional
    public OrderDetailsResponse updateOrderStatus(Long orderId, UpdateOrderStatusRequest request, Long managerUserId) {
        Order order = orderRepository.findWithDetailsById(orderId)
                .orElseThrow(() -> new NotFoundException("Замовлення з ID " + orderId + " не знайдено"));

        // Check expected status for optimistic concurrency protection (M3 7.2.2.8)
        if (request.getExpectedStatus() != null && order.getStatus() != request.getExpectedStatus()) {
            throw new ConflictException("Поточний статус замовлення '" + order.getStatus() + "' не відповідає очікуваному '" + request.getExpectedStatus() + "'");
        }

        OrderStatus currentStatus = order.getStatus();
        OrderStatus targetStatus = request.getStatus();

        if (currentStatus == OrderStatus.DELIVERED || currentStatus == OrderStatus.CANCELLED) {
            throw new BusinessRuleException("Замовлення у фінальному статусі '" + currentStatus + "' не може бути змінено");
        }

        // State Machine validation (SRS Table 3)
        boolean isValidTransition = switch (currentStatus) {
            case AWAITING_PAYMENT -> (targetStatus == OrderStatus.PAID)
                    || (targetStatus == OrderStatus.DELIVERED && order.getPaymentMethod() == PaymentMethod.ON_DELIVERY)
                    || (targetStatus == OrderStatus.CANCELLED);
            case PAID -> targetStatus == OrderStatus.DELIVERED || targetStatus == OrderStatus.CANCELLED;
            default -> false;
        };

        if (!isValidTransition) {
            throw new BusinessRuleException("Неприпустимий перехід статусу з '" + currentStatus + "' у '" + targetStatus + "'");
        }

        User manager = managerUserId != null ? userRepository.findById(managerUserId).orElse(null) : null;
        User customer = order.getUser();

        if (targetStatus == OrderStatus.CANCELLED) {
            // Restore book stock
            if (order.getItems() != null) {
                for (OrderItem item : order.getItems()) {
                    if (item.getBook() != null) {
                        Book book = item.getBook();
                        book.setStock(book.getStock() + item.getQuantity());
                        bookRepository.save(book);
                    }
                }
            }

            // Refund bonus points
            if (order.getPointsUsed() > 0 && customer != null) {
                customer.setBonusBalance(customer.getBonusBalance() + order.getPointsUsed());
                userRepository.save(customer);

                BonusTransaction refundTx = new BonusTransaction(customer, order.getPointsUsed(), BonusTransactionType.REFUND, order, null, null);
                bonusTransactionRepository.save(refundTx);
            }
        }

        order.setStatus(targetStatus);
        order.setStatusChangedAt(Instant.now());

        if (targetStatus == OrderStatus.DELIVERED) {
            order.setDeliveredAt(Instant.now());
            orderRepository.saveAndFlush(order);

            // 1. Award order bonus points (C8): floor(itemsTotal / 100)
            if (customer != null && order.getItemsTotal() != null) {
                int rewardPoints = order.getItemsTotal().divide(BigDecimal.valueOf(100), 0, RoundingMode.FLOOR).intValue();
                if (rewardPoints > 0) {
                    boolean alreadyAwarded = bonusTransactionRepository.findByOrderIdAndType(order.getId(), BonusTransactionType.ORDER_REWARD).isPresent();
                    if (!alreadyAwarded) {
                        customer.setBonusBalance(customer.getBonusBalance() + rewardPoints);
                        userRepository.save(customer);

                        BonusTransaction rewardTx = new BonusTransaction(customer, rewardPoints, BonusTransactionType.ORDER_REWARD, order, null, null);
                        bonusTransactionRepository.save(rewardTx);
                    }
                }
            }

            // 2. Check reading challenges (C7)
            if (customer != null) {
                List<Challenge> activeChallenges = challengeRepository.findByStatus(ChallengeStatus.ACTIVE);
                Instant now = Instant.now();
                for (Challenge challenge : activeChallenges) {
                    if (challengeCompletionRepository.existsByUserIdAndChallengeId(customer.getId(), challenge.getId())) {
                        continue;
                    }
                    if (challenge.getDeadline() != null && challenge.getDeadline().isBefore(now)) {
                        continue;
                    }

                    Long genreId = challenge.getGenre() != null ? challenge.getGenre().getId() : null;
                    int deliveredCount = orderItemRepository.countDeliveredBooksForChallenge(
                            customer.getId(),
                            challenge.getCreatedAt(),
                            challenge.getDeadline(),
                            genreId
                    );

                    if (deliveredCount >= challenge.getRequiredBooks()) {
                        ChallengeCompletion completion = new ChallengeCompletion(customer, challenge, Instant.now());
                        challengeCompletionRepository.save(completion);

                        if (challenge.getRewardPoints() > 0) {
                            customer.setBonusBalance(customer.getBonusBalance() + challenge.getRewardPoints());
                            userRepository.save(customer);

                            BonusTransaction chRewardTx = new BonusTransaction(customer, challenge.getRewardPoints(), BonusTransactionType.CHALLENGE_REWARD, null, null, challenge);
                            bonusTransactionRepository.save(chRewardTx);
                        }
                    }
                }
            }

            // 3. Gift log records (M9)
            if (order.getType() == OrderType.GIFT && order.getItems() != null) {
                for (OrderItem item : order.getItems()) {
                    GiftLog giftLog = new GiftLog(
                            order,
                            order.getInstitution(),
                            order.getInstitutionName(),
                            item.getTitleSnapshot(),
                            item.getQuantity(),
                            LocalDate.now()
                    );
                    giftLogRepository.save(giftLog);
                }
            }
        }

        Order savedOrder = orderRepository.save(order);

        OrderStatusHistory history = new OrderStatusHistory(savedOrder, currentStatus, targetStatus, manager, request.getComment());
        orderStatusHistoryRepository.save(history);

        List<OrderStatusHistoryResponse> historyList = orderStatusHistoryRepository.findByOrderIdOrderByChangedAtAsc(orderId)
                .stream()
                .map(OrderStatusHistoryResponse::fromEntity)
                .toList();

        return OrderDetailsResponse.fromEntity(savedOrder, historyList);
    }

    @Transactional
    public int cancelUnpaidOverdueOrders() {
        Instant before = Instant.now().minus(7, ChronoUnit.DAYS);
        List<Order> overdueOrders = orderRepository.findUnpaidOrdersOlderThan(before);
        int cancelledCount = 0;

        for (Order order : overdueOrders) {
            OrderStatus previousStatus = order.getStatus();
            order.setStatus(OrderStatus.CANCELLED);
            order.setStatusChangedAt(Instant.now());

            // Restore book stock
            if (order.getItems() != null) {
                for (OrderItem item : order.getItems()) {
                    if (item.getBook() != null) {
                        Book book = item.getBook();
                        book.setStock(book.getStock() + item.getQuantity());
                        bookRepository.save(book);
                    }
                }
            }

            // Refund bonus points
            if (order.getPointsUsed() > 0 && order.getUser() != null) {
                User customer = order.getUser();
                customer.setBonusBalance(customer.getBonusBalance() + order.getPointsUsed());
                userRepository.save(customer);

                BonusTransaction refundTx = new BonusTransaction(customer, order.getPointsUsed(), BonusTransactionType.REFUND, order, null, null);
                bonusTransactionRepository.save(refundTx);
            }

            OrderStatusHistory history = new OrderStatusHistory(order, previousStatus, OrderStatus.CANCELLED, null, "Автоматичне скасування: не оплачено понад 7 днів");
            orderStatusHistoryRepository.save(history);
            orderRepository.save(order);
            cancelledCount++;
        }

        log.info("Скасовано {} прострочених неоплачених замовлень", cancelledCount);
        return cancelledCount;
    }

    @Transactional
    public int cleanupArchivedOrders() {
        Instant before = Instant.now().minus(3 * 365, ChronoUnit.DAYS);
        List<Order> archivedOrders = orderRepository.findArchivedOrdersOlderThan(before);
        int deletedCount = archivedOrders.size();

        for (Order order : archivedOrders) {
            orderStatusHistoryRepository.deleteAll(orderStatusHistoryRepository.findByOrderIdOrderByChangedAtAsc(order.getId()));
            orderRepository.delete(order);
        }

        log.info("Видалено {} архівних замовлень (старших за 3 роки)", deletedCount);
        return deletedCount;
    }
}
