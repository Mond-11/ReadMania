package ua.readmania.service.cart;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ua.readmania.common.BusinessRuleException;
import ua.readmania.common.NotFoundException;
import ua.readmania.domain.cart.CartItem;
import ua.readmania.domain.cart.CartItemId;
import ua.readmania.domain.cart.CartItemRepository;
import ua.readmania.domain.catalog.Book;
import ua.readmania.domain.catalog.BookCoverRepository;
import ua.readmania.domain.catalog.BookRepository;
import ua.readmania.domain.user.User;
import ua.readmania.domain.user.UserRepository;
import ua.readmania.dto.cart.AddToCartRequest;
import ua.readmania.dto.cart.CartItemResponse;
import ua.readmania.dto.cart.CartResponse;
import ua.readmania.dto.cart.UpdateCartItemRequest;

@Service
public class CartService {

    private final CartItemRepository cartItemRepository;
    private final BookRepository bookRepository;
    private final BookCoverRepository bookCoverRepository;
    private final UserRepository userRepository;

    public CartService(CartItemRepository cartItemRepository,
                       BookRepository bookRepository,
                       BookCoverRepository bookCoverRepository,
                       UserRepository userRepository) {
        this.cartItemRepository = cartItemRepository;
        this.bookRepository = bookRepository;
        this.bookCoverRepository = bookCoverRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public CartResponse getCart(Long userId) {
        List<CartItem> items = cartItemRepository.findByUserId(userId);
        return buildCartResponse(items);
    }

    @Transactional
    public CartResponse addToCart(Long userId, AddToCartRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("Користувача не знайдено"));

        Book book = bookRepository.findById(request.getBookId())
                .orElseThrow(() -> new NotFoundException("Книгу не знайдено"));

        if (book.isDeleted()) {
            throw new BusinessRuleException("Книга видалена або недоступна для замовлення");
        }

        if (book.getStock() <= 0) {
            throw new BusinessRuleException("Книги '" + book.getTitle() + "' немає в наявності");
        }

        int requestedQuantity = request.getQuantity() > 0 ? request.getQuantity() : 1;

        Optional<CartItem> existingItemOpt = cartItemRepository.findByUserIdAndBookId(userId, book.getId());
        CartItem cartItem;
        if (existingItemOpt.isPresent()) {
            cartItem = existingItemOpt.get();
            int newQuantity = cartItem.getQuantity() + requestedQuantity;
            if (newQuantity > book.getStock()) {
                throw new BusinessRuleException("Запитувана кількість (" + newQuantity + ") перевищує наявний залишок на складі (" + book.getStock() + ")");
            }
            cartItem.setQuantity(newQuantity);
        } else {
            if (requestedQuantity > book.getStock()) {
                throw new BusinessRuleException("Запитувана кількість (" + requestedQuantity + ") перевищує наявний залишок на складі (" + book.getStock() + ")");
            }
            cartItem = new CartItem(user, book, requestedQuantity);
        }

        cartItemRepository.save(cartItem);
        return getCart(userId);
    }

    @Transactional
    public CartResponse updateCartItem(Long userId, Long bookId, UpdateCartItemRequest request) {
        CartItem cartItem = cartItemRepository.findByUserIdAndBookId(userId, bookId)
                .orElseThrow(() -> new NotFoundException("Позицію у кошику не знайдено"));

        Book book = cartItem.getBook();
        if (book.isDeleted() || book.getStock() <= 0) {
            throw new BusinessRuleException("Книга недоступна на складі");
        }

        if (request.getQuantity() < 1) {
            throw new BusinessRuleException("Кількість повинна бути не менше 1");
        }

        if (request.getQuantity() > book.getStock()) {
            throw new BusinessRuleException("Запитувана кількість (" + request.getQuantity() + ") перевищує наявний залишок на складі (" + book.getStock() + ")");
        }

        cartItem.setQuantity(request.getQuantity());
        cartItemRepository.save(cartItem);
        return getCart(userId);
    }

    @Transactional
    public CartResponse removeFromCart(Long userId, Long bookId) {
        cartItemRepository.deleteByUserIdAndBookId(userId, bookId);
        return getCart(userId);
    }

    @Transactional
    public void clearCart(Long userId) {
        cartItemRepository.deleteByUserId(userId);
    }

    private CartResponse buildCartResponse(List<CartItem> items) {
        if (items.isEmpty()) {
            return new CartResponse(new ArrayList<>(), 0, BigDecimal.ZERO, true);
        }

        Set<Long> bookIds = items.stream()
                .map(item -> item.getBook() != null ? item.getBook().getId() : null)
                .filter(id -> id != null)
                .collect(Collectors.toSet());

        Set<Long> booksWithCovers = bookCoverRepository.findAllById(bookIds).stream()
                .map(cover -> cover.getBookId())
                .collect(Collectors.toSet());

        List<CartItemResponse> itemResponses = new ArrayList<>();
        int totalItems = 0;
        BigDecimal totalPrice = BigDecimal.ZERO;
        boolean allAvailable = true;

        for (CartItem item : items) {
            Book book = item.getBook();
            boolean hasCover = book != null && booksWithCovers.contains(book.getId());
            CartItemResponse itemResponse = CartItemResponse.fromEntity(item, hasCover);
            itemResponses.add(itemResponse);

            if (!itemResponse.isAvailable() || item.getQuantity() > itemResponse.getStock()) {
                allAvailable = false;
            }

            totalItems += item.getQuantity();
            if (itemResponse.getSubtotal() != null) {
                totalPrice = totalPrice.add(itemResponse.getSubtotal());
            }
        }

        return new CartResponse(itemResponses, totalItems, totalPrice, allAvailable);
    }
}
