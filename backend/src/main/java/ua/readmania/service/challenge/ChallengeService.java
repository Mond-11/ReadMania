package ua.readmania.service.challenge;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ua.readmania.common.NotFoundException;
import ua.readmania.domain.challenge.Challenge;
import ua.readmania.domain.challenge.ChallengeCompletion;
import ua.readmania.domain.challenge.ChallengeCompletionRepository;
import ua.readmania.domain.challenge.ChallengeRepository;
import ua.readmania.domain.challenge.ChallengeStatus;
import ua.readmania.domain.order.OrderItemRepository;
import ua.readmania.dto.challenge.ChallengeResponse;

@Service
public class ChallengeService {

    private final ChallengeRepository challengeRepository;
    private final ChallengeCompletionRepository challengeCompletionRepository;
    private final OrderItemRepository orderItemRepository;

    public ChallengeService(ChallengeRepository challengeRepository,
                            ChallengeCompletionRepository challengeCompletionRepository,
                            OrderItemRepository orderItemRepository) {
        this.challengeRepository = challengeRepository;
        this.challengeCompletionRepository = challengeCompletionRepository;
        this.orderItemRepository = orderItemRepository;
    }

    @Transactional(readOnly = true)
    public List<ChallengeResponse> getActiveChallenges(Long userId) {
        List<Challenge> challenges = challengeRepository.findByStatusOrderByDeadlineAsc(ChallengeStatus.ACTIVE);
        Map<Long, ChallengeCompletion> completionsMap = Map.of();

        if (userId != null) {
            completionsMap = challengeCompletionRepository.findByUserId(userId).stream()
                    .collect(Collectors.toMap(c -> c.getChallenge().getId(), c -> c, (c1, c2) -> c1));
        }

        List<ChallengeResponse> result = new ArrayList<>();
        for (Challenge challenge : challenges) {
            ChallengeCompletion completion = completionsMap.get(challenge.getId());
            boolean isCompleted = completion != null;
            int progress = 0;

            if (userId != null) {
                if (isCompleted) {
                    progress = challenge.getRequiredBooks();
                } else {
                    Long genreId = challenge.getGenre() != null ? challenge.getGenre().getId() : null;
                    progress = orderItemRepository.countDeliveredBooksForChallenge(
                            userId,
                            challenge.getCreatedAt(),
                            challenge.getDeadline(),
                            genreId
                    );
                }
            }

            result.add(ChallengeResponse.fromEntity(
                    challenge,
                    progress,
                    isCompleted,
                    isCompleted ? completion.getCompletedAt() : null
            ));
        }

        return result;
    }

    @Transactional(readOnly = true)
    public ChallengeResponse getChallengeById(Long challengeId, Long userId) {
        Challenge challenge = challengeRepository.findById(challengeId)
                .orElseThrow(() -> new NotFoundException("Читацький виклик з ID " + challengeId + " не знайдено"));

        boolean isCompleted = false;
        ChallengeCompletion completion = null;
        int progress = 0;

        if (userId != null) {
            completion = challengeCompletionRepository.findByUserId(userId).stream()
                    .filter(c -> c.getChallenge().getId().equals(challengeId))
                    .findFirst()
                    .orElse(null);
            isCompleted = completion != null;

            if (isCompleted) {
                progress = challenge.getRequiredBooks();
            } else {
                Long genreId = challenge.getGenre() != null ? challenge.getGenre().getId() : null;
                progress = orderItemRepository.countDeliveredBooksForChallenge(
                        userId,
                        challenge.getCreatedAt(),
                        challenge.getDeadline(),
                        genreId
                );
            }
        }

        return ChallengeResponse.fromEntity(
                challenge,
                progress,
                isCompleted,
                isCompleted ? completion.getCompletedAt() : null
        );
    }
}
