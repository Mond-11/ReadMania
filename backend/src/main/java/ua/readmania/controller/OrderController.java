package ua.readmania.controller;

import jakarta.validation.Valid;
import java.security.Principal;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ua.readmania.common.NotFoundException;
import ua.readmania.domain.user.User;
import ua.readmania.domain.user.UserRepository;
import ua.readmania.dto.order.CreateGiftOrderRequest;
import ua.readmania.dto.order.CreateOrderRequest;
import ua.readmania.dto.order.OrderDetailsResponse;
import ua.readmania.dto.order.OrderPreviewRequest;
import ua.readmania.dto.order.OrderPreviewResponse;
import ua.readmania.dto.order.OrderSummaryResponse;
import ua.readmania.security.CustomUserDetails;
import ua.readmania.service.order.OrderService;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;
    private final UserRepository userRepository;

    public OrderController(OrderService orderService, UserRepository userRepository) {
        this.orderService = orderService;
        this.userRepository = userRepository;
    }

    @PostMapping("/preview")
    public ResponseEntity<OrderPreviewResponse> previewOrder(@AuthenticationPrincipal CustomUserDetails userDetails,
                                                             Principal principal,
                                                             @Valid @RequestBody(required = false) OrderPreviewRequest request) {
        Long userId = tryResolveUserId(userDetails, principal);
        OrderPreviewResponse response = orderService.previewOrder(userId, request);
        return ResponseEntity.ok(response);
    }

    @PostMapping
    public ResponseEntity<OrderDetailsResponse> createRegularOrder(@AuthenticationPrincipal CustomUserDetails userDetails,
                                                                   Principal principal,
                                                                   @Valid @RequestBody CreateOrderRequest request) {
        Long userId = resolveUserId(userDetails, principal);
        OrderDetailsResponse response = orderService.createRegularOrder(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/gift")
    public ResponseEntity<OrderDetailsResponse> createGiftOrder(@AuthenticationPrincipal CustomUserDetails userDetails,
                                                                Principal principal,
                                                                @Valid @RequestBody CreateGiftOrderRequest request) {
        Long userId = resolveUserId(userDetails, principal);
        OrderDetailsResponse response = orderService.createGiftOrder(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<Page<OrderSummaryResponse>> getUserOrders(@AuthenticationPrincipal CustomUserDetails userDetails,
                                                                    Principal principal,
                                                                    @PageableDefault(size = 10) Pageable pageable) {
        Long userId = resolveUserId(userDetails, principal);
        Page<OrderSummaryResponse> response = orderService.getUserOrders(userId, pageable);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrderDetailsResponse> getOrderById(@AuthenticationPrincipal CustomUserDetails userDetails,
                                                             Principal principal,
                                                             @PathVariable("id") Long id) {
        Long userId = resolveUserId(userDetails, principal);
        OrderDetailsResponse response = orderService.getUserOrderById(userId, id);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<OrderDetailsResponse> cancelOrder(@AuthenticationPrincipal CustomUserDetails userDetails,
                                                            Principal principal,
                                                            @PathVariable("id") Long id) {
        Long userId = resolveUserId(userDetails, principal);
        OrderDetailsResponse response = orderService.cancelOrder(userId, id);
        return ResponseEntity.ok(response);
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

    private Long tryResolveUserId(CustomUserDetails userDetails, Principal principal) {
        if (userDetails != null && userDetails.getId() != null) {
            return userDetails.getId();
        }
        if (principal != null) {
            return userRepository.findByEmailLower(principal.getName().toLowerCase())
                    .map(User::getId)
                    .orElse(null);
        }
        return null;
    }
}
