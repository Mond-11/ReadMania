package ua.readmania.service.challenge;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ua.readmania.common.BusinessRuleException;
import ua.readmania.common.NotFoundException;
import ua.readmania.domain.catalog.Genre;
import ua.readmania.domain.catalog.GenreRepository;
import ua.readmania.domain.challenge.Challenge;
import ua.readmania.domain.challenge.ChallengeCompletion;
import ua.readmania.domain.challenge.ChallengeCompletionRepository;
import ua.readmania.domain.challenge.ChallengeRepository;
import ua.readmania.domain.challenge.ChallengeStatus;
import ua.readmania.domain.order.OrderItemRepository;
import ua.readmania.domain.user.User;
import ua.readmania.domain.user.UserRepository;
import ua.readmania.dto.challenge.ChallengeResponse;
import ua.readmania.dto.challenge.CreateChallengeRequest;
import ua.readmania.dto.challenge.FinishChallengeResponse;
import ua.readmania.dto.challenge.UpdateChallengeRequest;

@Service
public class ChallengeService {

    private static final Logger log = LoggerFactory.getLogger(ChallengeService.class);

    private final ChallengeRepository challengeRepository;
    private final ChallengeCompletionRepository challengeCompletionRepository;
    private final OrderItemRepository orderItemRepository;
    private final GenreRepository genreRepository;
    private final UserRepository userRepository;

    public ChallengeService(ChallengeRepository challengeRepository,
                            ChallengeCompletionRepository challengeCompletionRepository,
                            OrderItemRepository orderItemRepository,
                            GenreRepository genreRepository,
                            UserRepository userRepository) {
        this.challengeRepository = challengeRepository;
        this.challengeCompletionRepository = challengeCompletionRepository;
        this.orderItemRepository = orderItemRepository;
        this.genreRepository = genreRepository;
        this.userRepository = userRepository;
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

    @Transactional(readOnly = true)
    public List<ChallengeResponse> getAllChallengesForManager(ChallengeStatus status) {
        List<Challenge> challenges;
        if (status != null) {
            challenges = challengeRepository.findByStatusOrderByCreatedAtDesc(status);
        } else {
            challenges = challengeRepository.findAllByOrderByCreatedAtDesc();
        }
        return challenges.stream()
                .map(c -> ChallengeResponse.fromEntity(c, 0, false, null))
                .toList();
    }

    @Transactional
    public ChallengeResponse createChallenge(CreateChallengeRequest request) {
        Genre genre = null;
        if (request.getGenreId() != null) {
            genre = genreRepository.findById(request.getGenreId())
                    .orElseThrow(() -> new NotFoundException("Жанр з ID " + request.getGenreId() + " не знайдено"));
        }

        Challenge challenge = new Challenge(
                request.getTitle(),
                request.getDescription(),
                request.getRequiredBooks(),
                genre,
                request.getDeadline(),
                request.getRewardPoints()
        );
        challenge.setStatus(ChallengeStatus.ACTIVE);

        Challenge savedChallenge = challengeRepository.save(challenge);
        log.info("Створено новий читацький виклик: '{}' (ID: {})", savedChallenge.getTitle(), savedChallenge.getId());
        return ChallengeResponse.fromEntity(savedChallenge, 0, false, null);
    }

    @Transactional
    public ChallengeResponse updateChallenge(Long id, UpdateChallengeRequest request) {
        Challenge challenge = challengeRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Читацький виклик з ID " + id + " не знайдено"));

        if (challenge.getStatus() != ChallengeStatus.ACTIVE) {
            throw new BusinessRuleException("Можна редагувати лише активні виклики");
        }

        Genre genre = null;
        if (request.getGenreId() != null) {
            genre = genreRepository.findById(request.getGenreId())
                    .orElseThrow(() -> new NotFoundException("Жанр з ID " + request.getGenreId() + " не знайдено"));
        }

        challenge.setTitle(request.getTitle());
        challenge.setDescription(request.getDescription());
        challenge.setRequiredBooks(request.getRequiredBooks());
        challenge.setGenre(genre);
        challenge.setDeadline(request.getDeadline());
        challenge.setRewardPoints(request.getRewardPoints());

        Challenge updatedChallenge = challengeRepository.save(challenge);
        log.info("Оновлено читацький виклик ID {}: '{}'", updatedChallenge.getId(), updatedChallenge.getTitle());
        return ChallengeResponse.fromEntity(updatedChallenge, 0, false, null);
    }

    @Transactional
    public FinishChallengeResponse finishChallengeEarly(Long id) {
        Challenge challenge = challengeRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Читацький виклик з ID " + id + " не знайдено"));

        if (challenge.getStatus() != ChallengeStatus.ACTIVE) {
            throw new BusinessRuleException("Можна достроково завершити лише активний виклик");
        }

        // C7 / M4.3: сповіщення користувачів, у яких прогрес > 0
        Long genreId = challenge.getGenre() != null ? challenge.getGenre().getId() : null;
        List<User> allUsers = userRepository.findAll();
        int notifiedCount = 0;

        for (User user : allUsers) {
            boolean alreadyCompleted = challengeCompletionRepository.existsByUserIdAndChallengeId(user.getId(), challenge.getId());
            if (!alreadyCompleted) {
                int progress = orderItemRepository.countDeliveredBooksForChallenge(
                        user.getId(),
                        challenge.getCreatedAt(),
                        challenge.getDeadline(),
                        genreId
                );
                if (progress > 0) {
                    notifiedCount++;
                    log.info("Сповіщення користувача {} (ID: {}): читацький виклик '{}' (ID: {}) достроково завершено менеджером. Поточний прогрес: {}/{}",
                            user.getEmail(), user.getId(), challenge.getTitle(), challenge.getId(), progress, challenge.getRequiredBooks());
                }
            }
        }

        challenge.setStatus(ChallengeStatus.CANCELLED);
        challenge.setFinishedAt(Instant.now());
        challengeRepository.save(challenge);

        log.info("Читацький виклик '{}' (ID: {}) достроково завершено. Сповіщено користувачів: {}",
                challenge.getTitle(), challenge.getId(), notifiedCount);

        return new FinishChallengeResponse(
                challenge.getId(),
                challenge.getTitle(),
                challenge.getStatus(),
                challenge.getFinishedAt(),
                notifiedCount,
                "Читацький виклик успішно завершено достроково. Сповіщено користувачів: " + notifiedCount
        );
    }
}
