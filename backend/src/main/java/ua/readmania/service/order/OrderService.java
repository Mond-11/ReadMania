package ua.readmania.service.order;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ua.readmania.common.BusinessRuleException;
import ua.readmania.common.ConflictException;
import ua.readmania.common.NotFoundException;
import ua.readmania.common.OrderStatusConflictException;
import ua.readmania.domain.cart.CartItem;
import ua.readmania.domain.cart.CartItemRepository;
import ua.readmania.domain.catalog.Book;
import ua.readmania.domain.catalog.BookRepository;
import ua.readmania.domain.institution.Institution;
import ua.readmania.domain.institution.InstitutionRepository;
import ua.readmania.domain.loyalty.BonusTransaction;
import ua.readmania.domain.loyalty.BonusTransactionRepository;
import ua.readmania.domain.loyalty.BonusTransactionType;
import ua.readmania.domain.loyalty.Rating;
import ua.readmania.domain.loyalty.RatingRepository;
import ua.readmania.domain.order.Order;
import ua.readmania.domain.order.OrderItem;
import ua.readmania.domain.order.OrderRepository;
import ua.readmania.domain.order.OrderStatus;
import ua.readmania.domain.order.OrderStatusHistory;
import ua.readmania.domain.order.OrderStatusHistoryRepository;
import ua.readmania.domain.order.OrderType;
import ua.readmania.domain.order.PaymentMethod;
import ua.readmania.domain.user.User;
import ua.readmania.domain.user.UserRepository;
import ua.readmania.domain.user.UserStatus;
import ua.readmania.dto.order.CreateGiftOrderRequest;
import ua.readmania.dto.order.CreateOrderRequest;
import ua.readmania.dto.order.OrderDetailsResponse;
import ua.readmania.dto.order.OrderItemRequest;
import ua.readmania.dto.order.OrderPreviewRequest;
import ua.readmania.dto.order.OrderPreviewResponse;
import ua.readmania.dto.order.OrderStatusHistoryResponse;
import ua.readmania.dto.order.OrderSummaryResponse;

@Service
public class OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderService.class);

    private final OrderRepository orderRepository;
    private final OrderStatusHistoryRepository orderStatusHistoryRepository;
    private final CartItemRepository cartItemRepository;
    private final BookRepository bookRepository;
    private final InstitutionRepository institutionRepository;
    private final UserRepository userRepository;
    private final BonusTransactionRepository bonusTransactionRepository;
    private final RatingRepository ratingRepository;
    private final ua.readmania.service.notification.NotificationService notificationService;

    public OrderService(OrderRepository orderRepository,
                        OrderStatusHistoryRepository orderStatusHistoryRepository,
                        CartItemRepository cartItemRepository,
                        BookRepository bookRepository,
                        InstitutionRepository institutionRepository,
                        UserRepository userRepository,
                        BonusTransactionRepository bonusTransactionRepository,
                        RatingRepository ratingRepository,
                        ua.readmania.service.notification.NotificationService notificationService) {
        this.orderRepository = orderRepository;
        this.orderStatusHistoryRepository = orderStatusHistoryRepository;
        this.cartItemRepository = cartItemRepository;
        this.bookRepository = bookRepository;
        this.institutionRepository = institutionRepository;
        this.userRepository = userRepository;
        this.bonusTransactionRepository = bonusTransactionRepository;
        this.ratingRepository = ratingRepository;
        this.notificationService = notificationService;
    }

    @Transactional(readOnly = true)
    public OrderPreviewResponse previewOrder(Long userId, OrderPreviewRequest request) {
        User user = null;
        int userBonusBalance = 0;
        if (userId != null) {
            user = userRepository.findById(userId).orElse(null);
            if (user != null) {
                userBonusBalance = user.getBonusBalance();
            }
        }

        List<OrderItemRequest> itemRequests = resolveItemRequests(userId, request != null ? request.getItems() : null);
        OrderPreviewResponse response = new OrderPreviewResponse();
        response.setUserBonusBalance(userBonusBalance);

        if (itemRequests.isEmpty()) {
            return response;
        }

        BigDecimal itemsTotal = BigDecimal.ZERO;
        List<OrderPreviewResponse.OrderItemPreviewDto> previewItems = new ArrayList<>();

        for (OrderItemRequest itemReq : itemRequests) {
            Book book = bookRepository.findById(itemReq.getBookId()).orElse(null);
            if (book == null) {
                continue;
            }
            int quantity = Math.max(1, itemReq.getQuantity());
            boolean available = !book.isDeleted() && book.getStock() >= quantity;
            BigDecimal price = book.getPrice() != null ? book.getPrice() : BigDecimal.ZERO;
            BigDecimal subtotal = price.multiply(BigDecimal.valueOf(quantity));

            previewItems.add(new OrderPreviewResponse.OrderItemPreviewDto(
                    book.getId(),
                    book.getTitle(),
                    price,
                    quantity,
                    subtotal,
                    available
            ));
            itemsTotal = itemsTotal.add(subtotal);
        }

        response.setItems(previewItems);
        response.setItemsTotal(itemsTotal);

        // Max points = min(userBonusBalance, floor((0.30 * itemsTotal) / 10))
        int maxAllowedByTotal = itemsTotal.multiply(BigDecimal.valueOf(0.30))
                .divide(BigDecimal.valueOf(10), 0, RoundingMode.FLOOR)
                .intValue();
        int maxBonusPoints = Math.max(0, Math.min(userBonusBalance, maxAllowedByTotal));
        response.setMaxBonusPoints(maxBonusPoints);

        int requestedPoints = request != null ? request.getBonusPoints() : 0;
        int pointsUsed = Math.max(0, Math.min(requestedPoints, maxBonusPoints));
        BigDecimal discountAmount = BigDecimal.valueOf(pointsUsed * 10L);
        BigDecimal totalToPay = itemsTotal.subtract(discountAmount);

        response.setPointsUsed(pointsUsed);
        response.setDiscountAmount(discountAmount);
        response.setTotalToPay(totalToPay);

        return response;
    }

    @Transactional
    public OrderDetailsResponse createRegularOrder(Long userId, CreateOrderRequest request) {
        User user = getUserOrThrow(userId);

        List<OrderItemRequest> itemRequests = resolveItemRequests(userId, request.getItems());
        if (itemRequests.isEmpty()) {
            throw new BusinessRuleException("Кошик або список позицій замовлення порожній");
        }

        BigDecimal itemsTotal = BigDecimal.ZERO;
        List<OrderItem> orderItems = new ArrayList<>();

        for (OrderItemRequest itemReq : itemRequests) {
            Book book = bookRepository.findById(itemReq.getBookId())
                    .orElseThrow(() -> new NotFoundException("Книгу з ID " + itemReq.getBookId() + " не знайдено"));

            if (book.isDeleted()) {
                throw new ConflictException("Книга '" + book.getTitle() + "' видалена або недоступна для замовлення");
            }

            int requestedQty = itemReq.getQuantity();
            if (requestedQty < 1) {
                throw new BusinessRuleException("Кількість товару повинна бути не менше 1");
            }

            if (book.getStock() < requestedQty) {
                throw new ConflictException("Недостатньо примірників книги '" + book.getTitle() + "' на складі. Доступно: " + book.getStock());
            }

            // Transactional decrement of stock (optimistic lock with @Version)
            book.setStock(book.getStock() - requestedQty);
            bookRepository.save(book);

            BigDecimal price = book.getPrice();
            BigDecimal subtotal = price.multiply(BigDecimal.valueOf(requestedQty));
            itemsTotal = itemsTotal.add(subtotal);

            OrderItem orderItem = new OrderItem(null, book, book.getTitle(), price, requestedQty);
            orderItems.add(orderItem);
        }

        // Validate and apply bonus points (C8)
        int requestedPoints = request.getBonusPoints();
        if (requestedPoints < 0) {
            throw new BusinessRuleException("Кількість бонусних балів не може бути від'ємною");
        }
        if (requestedPoints > user.getBonusBalance()) {
            throw new BusinessRuleException("Недостатньо бонусних балів на балансі. Доступно: " + user.getBonusBalance());
        }

        int maxAllowedPoints = itemsTotal.multiply(BigDecimal.valueOf(0.30))
                .divide(BigDecimal.valueOf(10), 0, RoundingMode.FLOOR)
                .intValue();
        if (requestedPoints > maxAllowedPoints) {
            throw new BusinessRuleException("Знижка бонусами не може перевищувати 30% вартості замовлення. Максимально дозволено: " + maxAllowedPoints + " балів");
        }

        int pointsUsed = requestedPoints;
        BigDecimal discountAmount = BigDecimal.valueOf(pointsUsed * 10L);
        BigDecimal totalToPay = itemsTotal.subtract(discountAmount);

        if (pointsUsed > 0) {
            user.setBonusBalance(user.getBonusBalance() - pointsUsed);
            userRepository.save(user);
        }

        OrderStatus initialStatus = OrderStatus.AWAITING_PAYMENT;

        Order order = new Order(
                user,
                OrderType.REGULAR,
                request.getPaymentMethod(),
                itemsTotal,
                discountAmount,
                pointsUsed
        );
        order.setStatus(initialStatus);
        order.setDeliveryMethod(request.getDeliveryMethod());
        order.setAddress(request.getAddress());
        if (request.getContacts() != null) {
            order.setRecipientName(request.getContacts().getRecipientName());
            order.setRecipientPhone(request.getContacts().getPhone());
        }

        for (OrderItem oi : orderItems) {
            order.addItem(oi);
        }

        Order savedOrder = orderRepository.save(order);

        if (pointsUsed > 0) {
            BonusTransaction spentTx = new BonusTransaction(user, -pointsUsed, BonusTransactionType.SPENT, savedOrder, null, null);
            bonusTransactionRepository.save(spentTx);
        }

        OrderStatusHistory history = new OrderStatusHistory(savedOrder, null, initialStatus, user, "Оформлення замовлення");
        orderStatusHistoryRepository.save(history);

        // Clear cart
        cartItemRepository.deleteByUserId(userId);

        log.info("Нове замовлення #{} оформлено користувачем {} на суму {} грн (метод оплати: {})",
                savedOrder.getId(), user.getEmail(), savedOrder.getTotalToPay(), savedOrder.getPaymentMethod());

        notificationService.sendNewOrderAlertToManagers(savedOrder);

        Map<Long, Integer> bookRatings = getBookRatingsForUser(userId, savedOrder.getItems());
        return OrderDetailsResponse.fromEntity(savedOrder, List.of(OrderStatusHistoryResponse.fromEntity(history)), bookRatings);
    }

    @Transactional
    public OrderDetailsResponse createGiftOrder(Long userId, CreateGiftOrderRequest request) {
        User user = getUserOrThrow(userId);

        Institution institution = institutionRepository.findById(request.getInstitutionId())
                .orElseThrow(() -> new NotFoundException("Благодійний заклад з ID " + request.getInstitutionId() + " не знайдено"));

        if (request.getPaymentMethod() != null && request.getPaymentMethod() != PaymentMethod.TRANSFER) {
            throw new BusinessRuleException("Для замовлень у дар закладам дозволено лише безготівкову оплату (TRANSFER)");
        }

        List<OrderItemRequest> itemRequests = resolveItemRequests(userId, request.getItems());
        if (itemRequests.isEmpty()) {
            throw new BusinessRuleException("Кошик або список позицій замовлення порожній");
        }

        BigDecimal itemsTotal = BigDecimal.ZERO;
        List<OrderItem> orderItems = new ArrayList<>();

        for (OrderItemRequest itemReq : itemRequests) {
            Book book = bookRepository.findById(itemReq.getBookId())
                    .orElseThrow(() -> new NotFoundException("Книгу з ID " + itemReq.getBookId() + " не знайдено"));

            if (book.isDeleted()) {
                throw new ConflictException("Книга '" + book.getTitle() + "' видалена або недоступна для замовлення");
            }

            int requestedQty = itemReq.getQuantity();
            if (requestedQty < 1) {
                throw new BusinessRuleException("Кількість товару повинна бути не менше 1");
            }

            if (book.getStock() < requestedQty) {
                throw new ConflictException("Недостатньо примірників книги '" + book.getTitle() + "' на складі. Доступно: " + book.getStock());
            }

            book.setStock(book.getStock() - requestedQty);
            bookRepository.save(book);

            BigDecimal price = book.getPrice();
            BigDecimal subtotal = price.multiply(BigDecimal.valueOf(requestedQty));
            itemsTotal = itemsTotal.add(subtotal);

            OrderItem orderItem = new OrderItem(null, book, book.getTitle(), price, requestedQty);
            orderItems.add(orderItem);
        }

        int requestedPoints = request.getBonusPoints();
        if (requestedPoints < 0) {
            throw new BusinessRuleException("Кількість бонусних балів не може бути від'ємною");
        }
        if (requestedPoints > user.getBonusBalance()) {
            throw new BusinessRuleException("Недостатньо бонусних балів на балансі. Доступно: " + user.getBonusBalance());
        }

        int maxAllowedPoints = itemsTotal.multiply(BigDecimal.valueOf(0.30))
                .divide(BigDecimal.valueOf(10), 0, RoundingMode.FLOOR)
                .intValue();
        if (requestedPoints > maxAllowedPoints) {
            throw new BusinessRuleException("Знижка бонусами не може перевищувати 30% вартості замовлення. Максимально дозволено: " + maxAllowedPoints + " балів");
        }

        int pointsUsed = requestedPoints;
        BigDecimal discountAmount = BigDecimal.valueOf(pointsUsed * 10L);
        BigDecimal totalToPay = itemsTotal.subtract(discountAmount);

        if (pointsUsed > 0) {
            user.setBonusBalance(user.getBonusBalance() - pointsUsed);
            userRepository.save(user);
        }

        Order order = new Order(
                user,
                OrderType.GIFT,
                PaymentMethod.TRANSFER,
                itemsTotal,
                discountAmount,
                pointsUsed
        );
        order.setStatus(OrderStatus.AWAITING_PAYMENT);
        order.setDeliveryMethod(null);
        order.setInstitution(institution);
        order.setInstitutionName(institution.getName());
        order.setInstitutionContactPerson(institution.getContactPerson());
        order.setAddress(institution.getAddress());
        order.setRecipientName(institution.getContactPerson());

        for (OrderItem oi : orderItems) {
            order.addItem(oi);
        }

        Order savedOrder = orderRepository.save(order);

        if (pointsUsed > 0) {
            BonusTransaction spentTx = new BonusTransaction(user, -pointsUsed, BonusTransactionType.SPENT, savedOrder, null, null);
            bonusTransactionRepository.save(spentTx);
        }

        OrderStatusHistory history = new OrderStatusHistory(savedOrder, null, OrderStatus.AWAITING_PAYMENT, user, "Оформлення замовлення в дар");
        orderStatusHistoryRepository.save(history);

        cartItemRepository.deleteByUserId(userId);

        log.info("Нове благодійне замовлення #{} оформлено для закладу '{}' на суму {} грн",
                savedOrder.getId(), institution.getName(), savedOrder.getTotalToPay());

        notificationService.sendNewOrderAlertToManagers(savedOrder);

        Map<Long, Integer> bookRatings = getBookRatingsForUser(userId, savedOrder.getItems());
        return OrderDetailsResponse.fromEntity(savedOrder, List.of(OrderStatusHistoryResponse.fromEntity(history)), bookRatings);
    }

    @Transactional(readOnly = true)
    public Page<OrderSummaryResponse> getUserOrders(Long userId, Pageable pageable) {
        return orderRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable)
                .map(OrderSummaryResponse::fromEntity);
    }

    @Transactional(readOnly = true)
    public OrderDetailsResponse getUserOrderById(Long userId, Long orderId) {
        Order order = orderRepository.findWithDetailsById(orderId)
                .orElseThrow(() -> new NotFoundException("Замовлення з ID " + orderId + " не знайдено"));

        if (order.getUser() == null || !order.getUser().getId().equals(userId)) {
            throw new NotFoundException("Замовлення з ID " + orderId + " не знайдено");
        }

        List<OrderStatusHistoryResponse> history = orderStatusHistoryRepository.findByOrderIdOrderByChangedAtAsc(orderId)
                .stream()
                .map(OrderStatusHistoryResponse::fromEntity)
                .toList();

        Map<Long, Integer> bookRatings = getBookRatingsForUser(userId, order.getItems());
        return OrderDetailsResponse.fromEntity(order, history, bookRatings);
    }

    @Transactional
    public OrderDetailsResponse cancelOrder(Long userId, Long orderId) {
        Order order = orderRepository.findWithDetailsById(orderId)
                .orElseThrow(() -> new NotFoundException("Замовлення з ID " + orderId + " не знайдено"));

        if (order.getUser() == null || !order.getUser().getId().equals(userId)) {
            throw new NotFoundException("Замовлення з ID " + orderId + " не знайдено");
        }

        // C3: Cancellation is permitted only for user's own orders in AWAITING_PAYMENT status with TRANSFER payment method
        if (order.getStatus() != OrderStatus.AWAITING_PAYMENT) {
            throw new OrderStatusConflictException("Неможливо скасувати замовлення у поточному статусі '" + order.getStatus() + "'", order.getStatus());
        }

        if (order.getPaymentMethod() != PaymentMethod.TRANSFER) {
            throw new BusinessRuleException("Скасування користувачем дозволено лише для замовлень з оплатою переказом (TRANSFER)");
        }

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
            User user = order.getUser();
            user.setBonusBalance(user.getBonusBalance() + order.getPointsUsed());
            userRepository.save(user);

            BonusTransaction refundTx = new BonusTransaction(user, order.getPointsUsed(), BonusTransactionType.REFUND, order, null, null);
            bonusTransactionRepository.save(refundTx);
        }

        OrderStatusHistory history = new OrderStatusHistory(order, previousStatus, OrderStatus.CANCELLED, order.getUser(), "Скасовано покупцем");
        orderStatusHistoryRepository.save(history);

        Order savedOrder;
        try {
            savedOrder = orderRepository.saveAndFlush(order);
        } catch (ObjectOptimisticLockingFailureException | jakarta.persistence.OptimisticLockException e) {
            Order actualOrder = orderRepository.findById(orderId).orElse(order);
            throw new OrderStatusConflictException("Замовлення було змінено іншим процесом. Поточний статус: " + actualOrder.getStatus(), actualOrder.getStatus(), e);
        }

        notificationService.sendOrderStatusChanged(savedOrder);

        List<OrderStatusHistoryResponse> historyList = orderStatusHistoryRepository.findByOrderIdOrderByChangedAtAsc(orderId)
                .stream()
                .map(OrderStatusHistoryResponse::fromEntity)
                .toList();

        Map<Long, Integer> bookRatings = getBookRatingsForUser(userId, savedOrder.getItems());
        return OrderDetailsResponse.fromEntity(savedOrder, historyList, bookRatings);
    }

    private Map<Long, Integer> getBookRatingsForUser(Long userId, List<OrderItem> items) {
        if (userId == null || items == null || items.isEmpty()) {
            return Map.of();
        }
        List<Long> bookIds = items.stream()
                .map(item -> item.getBook() != null ? item.getBook().getId() : null)
                .filter(Objects::nonNull)
                .toList();
        if (bookIds.isEmpty()) {
            return Map.of();
        }
        List<Rating> ratings = ratingRepository.findByUserIdAndBookIdIn(userId, bookIds);
        Map<Long, Integer> ratingMap = new HashMap<>();
        for (Rating r : ratings) {
            if (r.getBook() != null && r.getBook().getId() != null) {
                ratingMap.put(r.getBook().getId(), (int) r.getValue());
            }
        }
        return ratingMap;
    }

    private User getUserOrThrow(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("Користувача не знайдено"));
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new BusinessRuleException("Обліковий запис заблоковано: " + user.getBlockReason());
        }
        return user;
    }

    private List<OrderItemRequest> resolveItemRequests(Long userId, List<OrderItemRequest> requestedItems) {
        if (requestedItems != null && !requestedItems.isEmpty()) {
            return requestedItems;
        }
        if (userId != null) {
            List<CartItem> cartItems = cartItemRepository.findByUserId(userId);
            return cartItems.stream()
                    .map(ci -> new OrderItemRequest(ci.getBook().getId(), ci.getQuantity()))
                    .toList();
        }
        return new ArrayList<>();
    }
}
