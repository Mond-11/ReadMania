package ua.readmania.controller;

import jakarta.validation.Valid;
import java.security.Principal;
import java.util.Map;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ua.readmania.domain.order.OrderStatus;
import ua.readmania.domain.user.User;
import ua.readmania.domain.user.UserRepository;
import ua.readmania.dto.order.OrderDetailsResponse;
import ua.readmania.dto.order.OrderSummaryResponse;
import ua.readmania.dto.order.UpdateOrderStatusRequest;
import ua.readmania.security.CustomUserDetails;
import ua.readmania.service.order.ManagerOrderService;

@RestController
@RequestMapping("/api/manager/orders")
public class ManagerOrderController {

    private final ManagerOrderService managerOrderService;
    private final UserRepository userRepository;

    public ManagerOrderController(ManagerOrderService managerOrderService, UserRepository userRepository) {
        this.managerOrderService = managerOrderService;
        this.userRepository = userRepository;
    }

    @GetMapping
    public ResponseEntity<Page<OrderSummaryResponse>> getOrders(
            @RequestParam(name = "status", required = false) OrderStatus status,
            @RequestParam(name = "unpaidOverdue", required = false) Boolean unpaidOverdue,
            @RequestParam(name = "archived", required = false) Boolean archived,
            @PageableDefault(size = 10) Pageable pageable) {
        Page<OrderSummaryResponse> response = managerOrderService.getOrders(status, unpaidOverdue, archived, pageable);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrderDetailsResponse> getOrderById(@PathVariable("id") Long id) {
        OrderDetailsResponse response = managerOrderService.getOrderById(id);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<OrderDetailsResponse> updateOrderStatus(
            @PathVariable("id") Long id,
            @Valid @RequestBody UpdateOrderStatusRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            Principal principal) {
        Long managerUserId = resolveUserId(userDetails, principal);
        OrderDetailsResponse response = managerOrderService.updateOrderStatus(id, request, managerUserId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/cancel-unpaid-overdue")
    public ResponseEntity<Map<String, Object>> cancelUnpaidOverdueOrders() {
        int count = managerOrderService.cancelUnpaidOverdueOrders();
        return ResponseEntity.ok(Map.of(
                "message", "Скасовано прострочені неоплачені замовлення",
                "cancelledCount", count
        ));
    }

    @DeleteMapping("/archive")
    public ResponseEntity<Map<String, Object>> cleanupArchivedOrders() {
        int count = managerOrderService.cleanupArchivedOrders();
        return ResponseEntity.ok(Map.of(
                "message", "Очищено архівні замовлення",
                "deletedCount", count
        ));
    }

    private Long resolveUserId(CustomUserDetails userDetails, Principal principal) {
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
