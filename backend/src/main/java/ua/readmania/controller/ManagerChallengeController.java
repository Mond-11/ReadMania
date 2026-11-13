package ua.readmania.controller;

import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ua.readmania.domain.challenge.ChallengeStatus;
import ua.readmania.dto.challenge.ChallengeResponse;
import ua.readmania.dto.challenge.CreateChallengeRequest;
import ua.readmania.dto.challenge.FinishChallengeResponse;
import ua.readmania.dto.challenge.UpdateChallengeRequest;
import ua.readmania.service.challenge.ChallengeService;

@RestController
@RequestMapping("/api/manager/challenges")
public class ManagerChallengeController {

    private final ChallengeService challengeService;

    public ManagerChallengeController(ChallengeService challengeService) {
        this.challengeService = challengeService;
    }

    @GetMapping
    public ResponseEntity<List<ChallengeResponse>> getAllChallenges(@RequestParam(value = "status", required = false) ChallengeStatus status) {
        List<ChallengeResponse> response = challengeService.getAllChallengesForManager(status);
        return ResponseEntity.ok(response);
    }

    @PostMapping
    public ResponseEntity<ChallengeResponse> createChallenge(@Valid @RequestBody CreateChallengeRequest request) {
        ChallengeResponse response = challengeService.createChallenge(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ChallengeResponse> updateChallenge(@PathVariable("id") Long id,
                                                             @Valid @RequestBody UpdateChallengeRequest request) {
        ChallengeResponse response = challengeService.updateChallenge(id, request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/finish")
    public ResponseEntity<FinishChallengeResponse> finishChallengeEarly(@PathVariable("id") Long id) {
        FinishChallengeResponse response = challengeService.finishChallengeEarly(id);
        return ResponseEntity.ok(response);
    }
}
