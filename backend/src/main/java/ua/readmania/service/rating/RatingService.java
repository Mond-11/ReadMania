package ua.readmania.service.rating;

import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ua.readmania.common.BusinessRuleException;
import ua.readmania.common.ConflictException;
import ua.readmania.common.NotFoundException;
import ua.readmania.domain.catalog.Book;
import ua.readmania.domain.catalog.BookRepository;
import ua.readmania.domain.loyalty.BonusTransaction;
import ua.readmania.domain.loyalty.BonusTransactionRepository;
import ua.readmania.domain.loyalty.BonusTransactionType;
import ua.readmania.domain.loyalty.Rating;
import ua.readmania.domain.loyalty.RatingRepository;
import ua.readmania.domain.order.Order;
import ua.readmania.domain.order.OrderRepository;
import ua.readmania.domain.order.OrderStatus;
import ua.readmania.domain.user.User;
import ua.readmania.domain.user.UserRepository;
import ua.readmania.dto.rating.CreateRatingRequest;
import ua.readmania.dto.rating.RatingResponse;

@Service
public class RatingService {

    private final RatingRepository ratingRepository;
    private final BookRepository bookRepository;
    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final BonusTransactionRepository bonusTransactionRepository;

    public RatingService(RatingRepository ratingRepository,
                         BookRepository bookRepository,
                         OrderRepository orderRepository,
                         UserRepository userRepository,
                         BonusTransactionRepository bonusTransactionRepository) {
        this.ratingRepository = ratingRepository;
        this.bookRepository = bookRepository;
        this.orderRepository = orderRepository;
        this.userRepository = userRepository;
        this.bonusTransactionRepository = bonusTransactionRepository;
    }

    @Transactional
    public RatingResponse addRating(Long userId, CreateRatingRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("Користувача не знайдено"));

        Book book = bookRepository.findById(request.getBookId())
                .orElseThrow(() -> new NotFoundException("Книгу з ID " + request.getBookId() + " не знайдено"));

        if (ratingRepository.existsByUserIdAndBookId(userId, request.getBookId())) {
            throw new ConflictException("Ви вже оцінили цю книгу");
        }

        Order eligibleOrder;
        if (request.getOrderId() != null) {
            eligibleOrder = orderRepository.findWithDetailsById(request.getOrderId())
                    .orElseThrow(() -> new NotFoundException("Замовлення з ID " + request.getOrderId() + " не знайдено"));

            if (eligibleOrder.getUser() == null || !eligibleOrder.getUser().getId().equals(userId)) {
                throw new BusinessRuleException("Вказане замовлення не належить поточному користувачу");
            }
            if (eligibleOrder.getStatus() != OrderStatus.DELIVERED) {
                throw new BusinessRuleException("Оцінити можна лише книгу з отриманого замовлення (зі статусом DELIVERED)");
            }
            boolean containsBook = eligibleOrder.getItems() != null && eligibleOrder.getItems().stream()
                    .anyMatch(item -> item.getBook() != null && item.getBook().getId().equals(request.getBookId()));
            if (!containsBook) {
                throw new BusinessRuleException("Вказане замовлення не містить цю книгу");
            }
        } else {
            List<Order> deliveredOrders = orderRepository.findDeliveredOrdersByUserIdAndBookId(userId, request.getBookId());
            if (deliveredOrders.isEmpty()) {
                throw new BusinessRuleException("Оцінити можна лише книгу з отриманого замовлення (зі статусом DELIVERED)");
            }
            eligibleOrder = deliveredOrders.get(0);
        }

        Rating rating = new Rating(user, book, eligibleOrder, request.getValue().shortValue());
        Rating savedRating = ratingRepository.save(rating);

        // C6: Reward +1 bonus point for leaving a rating
        user.setBonusBalance(user.getBonusBalance() + 1);
        userRepository.save(user);

        BonusTransaction bonusTx = new BonusTransaction(
                user,
                1,
                BonusTransactionType.RATING_REWARD,
                eligibleOrder,
                book,
                null
        );
        bonusTransactionRepository.save(bonusTx);

        return RatingResponse.fromEntity(savedRating);
    }

    @Transactional(readOnly = true)
    public List<RatingResponse> getUserRatings(Long userId) {
        return ratingRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(RatingResponse::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<RatingResponse> getBookRatings(Long bookId) {
        return ratingRepository.findByBookIdOrderByCreatedAtDesc(bookId).stream()
                .map(RatingResponse::fromEntity)
                .toList();
    }
}
