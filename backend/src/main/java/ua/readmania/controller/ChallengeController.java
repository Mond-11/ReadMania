package ua.readmania.controller;

import java.security.Principal;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ua.readmania.domain.user.User;
import ua.readmania.domain.user.UserRepository;
import ua.readmania.dto.challenge.ChallengeResponse;
import ua.readmania.security.CustomUserDetails;
import ua.readmania.service.challenge.ChallengeService;

@RestController
@RequestMapping("/api/challenges")
public class ChallengeController {

    private final ChallengeService challengeService;
    private final UserRepository userRepository;

    public ChallengeController(ChallengeService challengeService, UserRepository userRepository) {
        this.challengeService = challengeService;
        this.userRepository = userRepository;
    }

    @GetMapping
    public ResponseEntity<List<ChallengeResponse>> getChallenges(@AuthenticationPrincipal CustomUserDetails userDetails,
                                                                 Principal principal) {
        Long userId = tryResolveUserId(userDetails, principal);
        List<ChallengeResponse> response = challengeService.getActiveChallenges(userId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ChallengeResponse> getChallengeById(@PathVariable("id") Long id,
                                                              @AuthenticationPrincipal CustomUserDetails userDetails,
                                                              Principal principal) {
        Long userId = tryResolveUserId(userDetails, principal);
        ChallengeResponse response = challengeService.getChallengeById(id, userId);
        return ResponseEntity.ok(response);
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
