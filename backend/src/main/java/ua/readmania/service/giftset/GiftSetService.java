package ua.readmania.service.giftset;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ua.readmania.common.BusinessRuleException;
import ua.readmania.common.NotFoundException;
import ua.readmania.domain.catalog.Book;
import ua.readmania.domain.catalog.BookRepository;
import ua.readmania.domain.catalog.Genre;
import ua.readmania.domain.catalog.GenreRepository;
import ua.readmania.dto.catalog.GenreResponse;
import ua.readmania.dto.giftset.GenerateGiftSetRequest;
import ua.readmania.dto.giftset.GiftSetBookItem;
import ua.readmania.dto.giftset.GiftSetResponse;
import ua.readmania.dto.giftset.ReplaceCandidatesResponse;
import ua.readmania.dto.giftset.ReplaceGiftSetBookRequest;

@Service
public class GiftSetService {

    private final BookRepository bookRepository;
    private final GenreRepository genreRepository;

    public GiftSetService(BookRepository bookRepository, GenreRepository genreRepository) {
        this.bookRepository = bookRepository;
        this.genreRepository = genreRepository;
    }

    @Transactional(readOnly = true)
    public GiftSetResponse generateGiftSet(GenerateGiftSetRequest request) {
        // C9: Бюджет повинен бути кратним 50 грн
        if (request.getBudget() == null || request.getBudget().remainder(BigDecimal.valueOf(50)).compareTo(BigDecimal.ZERO) != 0) {
            throw new BusinessRuleException("Бюджет повинен бути кратним 50 грн (наприклад 200, 250, 300 тощо)");
        }

        List<Genre> selectedGenres = genreRepository.findAllById(request.getGenreIds());
        if (selectedGenres.isEmpty()) {
            throw new NotFoundException("Жодного з обраних жанрів не знайдено");
        }

        List<Book> availableBooks = bookRepository.findAvailableByGenreIds(request.getGenreIds());

        int requiredCount = request.getBookCount() != null ? request.getBookCount() : 1;

        // Причина відмови 1: Недостатньо книг у наявності за обраними жанрами
        if (availableBooks.isEmpty() || availableBooks.size() < requiredCount) {
            throw new BusinessRuleException("Недостатньо книг у наявності за обраними жанрами для формування подарункового набору. Спробуйте розширити перелік жанрів.");
        }

        // Причина відмови 2: Вказаного бюджету недостатньо за обраними критеріями
        List<Book> sortedByPrice = availableBooks.stream()
                .sorted(Comparator.comparing(Book::getPrice))
                .toList();

        BigDecimal minRequiredPrice = BigDecimal.ZERO;
        for (int i = 0; i < requiredCount; i++) {
            minRequiredPrice = minRequiredPrice.add(sortedByPrice.get(i).getPrice());
        }

        if (minRequiredPrice.compareTo(request.getBudget()) > 0) {
            throw new BusinessRuleException("Вказаного бюджету (" + request.getBudget() + " грн) недостатньо для формування подарункового набору за обраними критеріями. Мінімальна вартість становить " + minRequiredPrice + " грн.");
        }

        // Алгоритм рандомізованого підбору з урахуванням виключень
        List<Book> bestCombination = findBestCombination(availableBooks, request.getBudget(), request.getBookCount(), request.getExcludeBookIds());

        if (bestCombination.isEmpty()) {
            throw new BusinessRuleException("Не вдалося підібрати комбінацію книг під вказаний бюджет. Спробуйте змінити бюджет або кількість книг.");
        }

        BigDecimal totalPrice = bestCombination.stream()
                .map(Book::getPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal remainingBudget = request.getBudget().subtract(totalPrice);

        List<GiftSetBookItem> items = bestCombination.stream()
                .map(GiftSetBookItem::fromEntity)
                .toList();

        List<GenreResponse> genres = selectedGenres.stream()
                .map(GenreResponse::fromEntity)
                .toList();

        return new GiftSetResponse(items, totalPrice, request.getBudget(), remainingBudget, genres);
    }

    @Transactional(readOnly = true)
    public GiftSetResponse replaceBook(ReplaceGiftSetBookRequest request) {
        if (!request.getCurrentBookIds().contains(request.getBookIdToReplace())) {
            throw new BusinessRuleException("Книга для заміни не присутня у поточному наборі");
        }

        List<Book> currentBooks = bookRepository.findAllById(request.getCurrentBookIds());
        if (currentBooks.size() != request.getCurrentBookIds().size()) {
            throw new NotFoundException("Деякі книги з поточного набору не знайдено");
        }

        BigDecimal otherBooksTotal = currentBooks.stream()
                .filter(b -> !b.getId().equals(request.getBookIdToReplace()))
                .map(Book::getPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal maxPriceAllowed = request.getBudget().subtract(otherBooksTotal);
        if (maxPriceAllowed.compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessRuleException("Сума інших книг у наборі перевищує вказаний бюджет");
        }

        List<Book> candidates = bookRepository.findCandidatesForReplace(
                request.getGenreIds(),
                request.getCurrentBookIds(),
                maxPriceAllowed
        );

        if (candidates.isEmpty()) {
            throw new BusinessRuleException("Немає доступних альтернативних книг за обраними жанрами у межах залишкового бюджету (" + maxPriceAllowed + " грн).");
        }

        Book replacementBook = candidates.get(ThreadLocalRandom.current().nextInt(candidates.size()));

        List<Book> updatedBooks = new ArrayList<>();
        for (Book book : currentBooks) {
            if (book.getId().equals(request.getBookIdToReplace())) {
                updatedBooks.add(replacementBook);
            } else {
                updatedBooks.add(book);
            }
        }

        BigDecimal totalPrice = updatedBooks.stream()
                .map(Book::getPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal remainingBudget = request.getBudget().subtract(totalPrice);

        List<GiftSetBookItem> items = updatedBooks.stream()
                .map(GiftSetBookItem::fromEntity)
                .toList();

        List<GenreResponse> genres = genreRepository.findAllById(request.getGenreIds()).stream()
                .map(GenreResponse::fromEntity)
                .toList();

        return new GiftSetResponse(items, totalPrice, request.getBudget(), remainingBudget, genres);
    }

    @Transactional(readOnly = true)
    public ReplaceCandidatesResponse getReplaceCandidates(ReplaceGiftSetBookRequest request) {
        if (!request.getCurrentBookIds().contains(request.getBookIdToReplace())) {
            throw new BusinessRuleException("Книга для заміни не присутня у поточному наборі");
        }

        List<Book> currentBooks = bookRepository.findAllById(request.getCurrentBookIds());
        BigDecimal otherBooksTotal = currentBooks.stream()
                .filter(b -> !b.getId().equals(request.getBookIdToReplace()))
                .map(Book::getPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal maxPriceAllowed = request.getBudget().subtract(otherBooksTotal);
        if (maxPriceAllowed.compareTo(BigDecimal.ZERO) < 0) {
            return new ReplaceCandidatesResponse(List.of(), BigDecimal.ZERO);
        }

        List<Book> candidates = bookRepository.findCandidatesForReplace(
                request.getGenreIds(),
                request.getCurrentBookIds(),
                maxPriceAllowed
        );

        List<GiftSetBookItem> items = candidates.stream()
                .map(GiftSetBookItem::fromEntity)
                .toList();

        return new ReplaceCandidatesResponse(items, maxPriceAllowed);
    }

    private List<Book> findBestCombination(List<Book> availableBooks, BigDecimal budget, Integer exactCount, List<Long> excludeBookIds) {
        List<Book> pool = new ArrayList<>(availableBooks);
        Set<Long> excluded = new HashSet<>(excludeBookIds != null ? excludeBookIds : List.of());

        List<Book> bestCandidate = null;
        List<Book> fallbackCandidate = null;
        int minOverlapCount = Integer.MAX_VALUE;

        int maxAttempts = 30;
        for (int attempt = 0; attempt < maxAttempts; attempt++) {
            Collections.shuffle(pool, ThreadLocalRandom.current());
            List<Book> candidate = pickCandidateCombination(pool, budget, exactCount);

            if (candidate.isEmpty()) {
                continue;
            }

            if (exactCount != null && candidate.size() != exactCount) {
                continue;
            }

            BigDecimal totalCandidatePrice = candidate.stream().map(Book::getPrice).reduce(BigDecimal.ZERO, BigDecimal::add);
            if (totalCandidatePrice.compareTo(budget) > 0) {
                continue;
            }

            Set<Long> candidateIds = candidate.stream().map(Book::getId).collect(Collectors.toSet());

            if (fallbackCandidate == null) {
                fallbackCandidate = candidate;
            }

            if (excluded.isEmpty()) {
                return candidate;
            }

            // Пріоритет 1 (ідеальний збіг): набір валідний і не має жодного перетину з excluded
            if (Collections.disjoint(candidateIds, excluded)) {
                return candidate;
            }

            // Пріоритет 2 (часткова новизна): набір валідний і хоча б частково відрізняється від попереднього (!candidateIds.equals(excluded))
            if (!candidateIds.equals(excluded)) {
                int overlap = 0;
                for (Long id : candidateIds) {
                    if (excluded.contains(id)) {
                        overlap++;
                    }
                }
                if (bestCandidate == null || overlap < minOverlapCount) {
                    bestCandidate = candidate;
                    minOverlapCount = overlap;
                }
            }
        }

        if (bestCandidate != null) {
            return bestCandidate;
        }

        if (fallbackCandidate != null) {
            return fallbackCandidate;
        }

        // Пріоритет 3 (безальтернативний fallback)
        if (exactCount != null) {
            List<Book> sortedCheapest = pool.stream()
                    .sorted(Comparator.comparing(Book::getPrice))
                    .toList();
            List<Book> fallback = findCombinationWithCount(sortedCheapest, budget, exactCount);
            if (!fallback.isEmpty()) {
                return fallback;
            }
        } else {
            List<Book> sortedCheapest = pool.stream()
                    .sorted(Comparator.comparing(Book::getPrice))
                    .toList();
            List<Book> fallback = new ArrayList<>();
            BigDecimal sum = BigDecimal.ZERO;
            for (Book book : sortedCheapest) {
                if (sum.add(book.getPrice()).compareTo(budget) <= 0) {
                    fallback.add(book);
                    sum = sum.add(book.getPrice());
                } else {
                    break;
                }
            }
            if (!fallback.isEmpty()) {
                return fallback;
            }
        }

        return List.of();
    }

    private List<Book> pickCandidateCombination(List<Book> pool, BigDecimal budget, Integer exactCount) {
        if (exactCount != null) {
            return findCombinationWithCount(pool, budget, exactCount);
        } else {
            List<Book> currentCombination = new ArrayList<>();
            BigDecimal currentSum = BigDecimal.ZERO;
            int targetSize = Math.min(pool.size(), Math.max(1, ThreadLocalRandom.current().nextInt(Math.min(5, pool.size()) + 1)));
            for (Book book : pool) {
                if (currentSum.add(book.getPrice()).compareTo(budget) <= 0) {
                    currentCombination.add(book);
                    currentSum = currentSum.add(book.getPrice());
                    if (currentCombination.size() >= targetSize) {
                        break;
                    }
                }
            }
            return currentCombination;
        }
    }

    private List<Book> findCombinationWithCount(List<Book> pool, BigDecimal budget, int count) {
        List<Book> result = new ArrayList<>();
        if (findCombinationBacktrack(pool, 0, budget, count, BigDecimal.ZERO, result)) {
            return result;
        }
        return List.of();
    }

    private boolean findCombinationBacktrack(List<Book> pool, int index, BigDecimal budget, int count, BigDecimal currentSum, List<Book> current) {
        if (current.size() == count) {
            return true;
        }
        if (index >= pool.size() || current.size() + (pool.size() - index) < count) {
            return false;
        }
        for (int i = index; i < pool.size(); i++) {
            Book book = pool.get(i);
            BigDecimal nextSum = currentSum.add(book.getPrice());
            if (nextSum.compareTo(budget) <= 0) {
                current.add(book);
                if (findCombinationBacktrack(pool, i + 1, budget, count, nextSum, current)) {
                    return true;
                }
                current.remove(current.size() - 1);
            }
        }
        return false;
    }
}
