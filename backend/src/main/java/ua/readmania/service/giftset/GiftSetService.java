package ua.readmania.service.giftset;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Random;
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
    private final Random random = new Random();

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

        // Алгоритм рандомізованого підбору
        List<Book> bestCombination = findBestCombination(availableBooks, request.getBudget(), request.getBookCount());

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

        List<Book> shuffledCandidates = new ArrayList<>(candidates);
        Collections.shuffle(shuffledCandidates, random);
        Book replacementBook = shuffledCandidates.get(0);

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

    private List<Book> findBestCombination(List<Book> availableBooks, BigDecimal budget, Integer exactCount) {
        List<Book> pool = new ArrayList<>(availableBooks);
        List<Book> bestResult = new ArrayList<>();
        BigDecimal bestSum = BigDecimal.ZERO;

        int iterations = 100;
        for (int i = 0; i < iterations; i++) {
            Collections.shuffle(pool, random);
            List<Book> currentCombination = new ArrayList<>();
            BigDecimal currentSum = BigDecimal.ZERO;

            if (exactCount != null) {
                for (Book book : pool) {
                    if (currentCombination.size() < exactCount) {
                        currentCombination.add(book);
                        currentSum = currentSum.add(book.getPrice());
                    }
                }
                if (currentCombination.size() == exactCount && currentSum.compareTo(budget) <= 0) {
                    if (currentSum.compareTo(bestSum) > 0) {
                        bestSum = currentSum;
                        bestResult = new ArrayList<>(currentCombination);
                    }
                }
            } else {
                int targetSize = Math.min(pool.size(), Math.max(2, random.nextInt(Math.min(5, pool.size()) + 1)));
                for (Book book : pool) {
                    if (currentSum.add(book.getPrice()).compareTo(budget) <= 0) {
                        currentCombination.add(book);
                        currentSum = currentSum.add(book.getPrice());
                        if (currentCombination.size() >= targetSize) {
                            break;
                        }
                    }
                }
                if (!currentCombination.isEmpty() && currentSum.compareTo(budget) <= 0) {
                    if (currentSum.compareTo(bestSum) > 0 || (currentSum.compareTo(bestSum) == 0 && currentCombination.size() > bestResult.size())) {
                        bestSum = currentSum;
                        bestResult = new ArrayList<>(currentCombination);
                    }
                }
            }
        }

        // Fallback greedy selection if random shuffle failed to find exactCount combination
        if (bestResult.isEmpty()) {
            if (exactCount != null) {
                List<Book> sortedCheapest = pool.stream()
                        .sorted(Comparator.comparing(Book::getPrice))
                        .limit(exactCount)
                        .toList();
                BigDecimal sum = sortedCheapest.stream().map(Book::getPrice).reduce(BigDecimal.ZERO, BigDecimal::add);
                if (sortedCheapest.size() == exactCount && sum.compareTo(budget) <= 0) {
                    bestResult = new ArrayList<>(sortedCheapest);
                }
            } else {
                List<Book> sortedCheapest = pool.stream()
                        .sorted(Comparator.comparing(Book::getPrice))
                        .toList();
                BigDecimal sum = BigDecimal.ZERO;
                for (Book book : sortedCheapest) {
                    if (sum.add(book.getPrice()).compareTo(budget) <= 0) {
                        bestResult.add(book);
                        sum = sum.add(book.getPrice());
                    } else {
                        break;
                    }
                }
            }
        }

        return bestResult;
    }
}
