package ua.readmania.controller;

import jakarta.validation.Valid;
import java.security.Principal;
import java.util.List;
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
import ua.readmania.dto.rating.CreateRatingRequest;
import ua.readmania.dto.rating.RatingResponse;
import ua.readmania.security.CustomUserDetails;
import ua.readmania.service.rating.RatingService;

@RestController
@RequestMapping("/api/ratings")
public class RatingController {

    private final RatingService ratingService;
    private final UserRepository userRepository;

    public RatingController(RatingService ratingService, UserRepository userRepository) {
        this.ratingService = ratingService;
        this.userRepository = userRepository;
    }

    @PostMapping
    public ResponseEntity<RatingResponse> addRating(@AuthenticationPrincipal CustomUserDetails userDetails,
                                                    Principal principal,
                                                    @Valid @RequestBody CreateRatingRequest request) {
        Long userId = resolveUserId(userDetails, principal);
        RatingResponse response = ratingService.addRating(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/me")
    public ResponseEntity<List<RatingResponse>> getMyRatings(@AuthenticationPrincipal CustomUserDetails userDetails,
                                                             Principal principal) {
        Long userId = resolveUserId(userDetails, principal);
        List<RatingResponse> response = ratingService.getUserRatings(userId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/book/{bookId}")
    public ResponseEntity<List<RatingResponse>> getBookRatings(@PathVariable("bookId") Long bookId) {
        List<RatingResponse> response = ratingService.getBookRatings(bookId);
        return ResponseEntity.ok(response);
    }

    private Long resolveUserId(CustomUserDetails userDetails, Principal principal) {
        if (userDetails != null && userDetails.getId() != null) {
            return userDetails.getId();
        }
        if (principal != null) {
            return userRepository.findByEmailLower(principal.getName().toLowerCase())
                    .map(User::getId)
                    .orElseThrow(() -> new NotFoundException("Користувача не знайдено"));
        }
        throw new NotFoundException("Користувача не знайдено");
    }
}
