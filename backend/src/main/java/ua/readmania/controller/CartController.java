package ua.readmania.controller;

import jakarta.validation.Valid;
import java.security.Principal;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ua.readmania.common.NotFoundException;
import ua.readmania.domain.user.User;
import ua.readmania.domain.user.UserRepository;
import ua.readmania.dto.cart.AddToCartRequest;
import ua.readmania.dto.cart.CartResponse;
import ua.readmania.dto.cart.UpdateCartItemRequest;
import ua.readmania.security.CustomUserDetails;
import ua.readmania.service.cart.CartService;

@RestController
@RequestMapping("/api/cart")
public class CartController {

    private final CartService cartService;
    private final UserRepository userRepository;

    public CartController(CartService cartService, UserRepository userRepository) {
        this.cartService = cartService;
        this.userRepository = userRepository;
    }

    @GetMapping
    public ResponseEntity<CartResponse> getCart(@AuthenticationPrincipal CustomUserDetails userDetails,
                                                Principal principal) {
        Long userId = resolveUserId(userDetails, principal);
        CartResponse response = cartService.getCart(userId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/items")
    public ResponseEntity<CartResponse> addToCart(@AuthenticationPrincipal CustomUserDetails userDetails,
                                                  Principal principal,
                                                  @Valid @RequestBody AddToCartRequest request) {
        Long userId = resolveUserId(userDetails, principal);
        CartResponse response = cartService.addToCart(userId, request);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/items/{bookId}")
    public ResponseEntity<CartResponse> updateCartItem(@AuthenticationPrincipal CustomUserDetails userDetails,
                                                       Principal principal,
                                                       @PathVariable("bookId") Long bookId,
                                                       @Valid @RequestBody UpdateCartItemRequest request) {
        Long userId = resolveUserId(userDetails, principal);
        CartResponse response = cartService.updateCartItem(userId, bookId, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/items/{bookId}")
    public ResponseEntity<CartResponse> removeFromCart(@AuthenticationPrincipal CustomUserDetails userDetails,
                                                       Principal principal,
                                                       @PathVariable("bookId") Long bookId) {
        Long userId = resolveUserId(userDetails, principal);
        CartResponse response = cartService.removeFromCart(userId, bookId);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping
    public ResponseEntity<Void> clearCart(@AuthenticationPrincipal CustomUserDetails userDetails,
                                          Principal principal) {
        Long userId = resolveUserId(userDetails, principal);
        cartService.clearCart(userId);
        return ResponseEntity.noContent().build();
    }

    private Long resolveUserId(CustomUserDetails userDetails, Principal principal) {
        if (userDetails != null && userDetails.getId() != null) {
            return userDetails.getId();
        }
        if (principal != null) {
            User user = userRepository.findByEmailLower(principal.getName().toLowerCase())
                    .orElseThrow(() -> new NotFoundException("Користувача не знайдено"));
            return user.getId();
        }
        throw new NotFoundException("Користувача не авторизовано");
    }
}
