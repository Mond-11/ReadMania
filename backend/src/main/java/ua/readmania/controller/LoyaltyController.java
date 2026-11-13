package ua.readmania.controller;

import java.security.Principal;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ua.readmania.common.NotFoundException;
import ua.readmania.domain.user.User;
import ua.readmania.domain.user.UserRepository;
import ua.readmania.dto.loyalty.LoyaltyBalanceResponse;
import ua.readmania.security.CustomUserDetails;
import ua.readmania.service.loyalty.LoyaltyService;

@RestController
@RequestMapping("/api/loyalty")
public class LoyaltyController {

    private final LoyaltyService loyaltyService;
    private final UserRepository userRepository;

    public LoyaltyController(LoyaltyService loyaltyService, UserRepository userRepository) {
        this.loyaltyService = loyaltyService;
        this.userRepository = userRepository;
    }

    @GetMapping({"/balance", "/transactions"})
    public ResponseEntity<LoyaltyBalanceResponse> getLoyaltyBalance(@AuthenticationPrincipal CustomUserDetails userDetails,
                                                                    Principal principal) {
        Long userId = resolveUserId(userDetails, principal);
        LoyaltyBalanceResponse response = loyaltyService.getLoyaltyBalance(userId);
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
}
