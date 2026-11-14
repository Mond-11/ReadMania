package ua.readmania.service.analytics;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ua.readmania.common.BusinessRuleException;
import ua.readmania.dto.manager.analytics.ChartItemDto;
import ua.readmania.dto.manager.analytics.DemandAnalyticsResponse;
import ua.readmania.dto.manager.analytics.RatingAnalyticsResponse;
import ua.readmania.dto.manager.analytics.RatingChartItemDto;
import ua.readmania.dto.manager.analytics.StockAnalyticsResponse;
import ua.readmania.dto.manager.analytics.StockItemDto;

@Service
@Transactional(readOnly = true)
public class AnalyticsService {

    private final JdbcClient jdbcClient;

    public AnalyticsService(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    public DemandAnalyticsResponse getDemandAnalytics(LocalDate from, LocalDate to) {
        PeriodDates period = validateAndResolvePeriod(from, to);

        String genresSql = """
                SELECT g.id AS id, g.name AS name, COALESCE(SUM(oi.quantity), 0) AS ordered_quantity
                FROM order_items oi
                JOIN orders o ON oi.order_id = o.id
                JOIN books b ON oi.book_id = b.id
                JOIN genres g ON b.genre_id = g.id
                WHERE o.created_at >= :fromStart AND o.created_at <= :toEnd AND o.status <> 'CANCELLED'
                GROUP BY g.id, g.name
                ORDER BY ordered_quantity DESC, g.name ASC
                """;

        List<ChartItemDto> byGenres = jdbcClient.sql(genresSql)
                .param("fromStart", period.fromStart())
                .param("toEnd", period.toEnd())
                .query((rs, rowNum) -> new ChartItemDto(
                        rs.getLong("id"),
                        rs.getString("name"),
                        rs.getLong("ordered_quantity")
                ))
                .list();

        String booksSql = """
                SELECT oi.book_id AS id, COALESCE(b.title, oi.book_title) AS name, COALESCE(SUM(oi.quantity), 0) AS ordered_quantity
                FROM order_items oi
                JOIN orders o ON oi.order_id = o.id
                LEFT JOIN books b ON oi.book_id = b.id
                WHERE o.created_at >= :fromStart AND o.created_at <= :toEnd AND o.status <> 'CANCELLED'
                GROUP BY oi.book_id, COALESCE(b.title, oi.book_title)
                ORDER BY ordered_quantity DESC, name ASC
                """;

        List<ChartItemDto> byBooks = jdbcClient.sql(booksSql)
                .param("fromStart", period.fromStart())
                .param("toEnd", period.toEnd())
                .query((rs, rowNum) -> new ChartItemDto(
                        rs.getLong("id"),
                        rs.getString("name"),
                        rs.getLong("ordered_quantity")
                ))
                .list();

        return new DemandAnalyticsResponse(byGenres, byBooks);
    }

    public StockAnalyticsResponse getStockAnalytics() {
        String genresSql = """
                SELECT g.id AS id, g.name AS name, COALESCE(SUM(b.stock), 0) AS stock
                FROM genres g
                LEFT JOIN books b ON b.genre_id = g.id AND b.deleted_at IS NULL
                GROUP BY g.id, g.name
                ORDER BY stock DESC, g.name ASC
                """;

        List<StockItemDto> byGenres = jdbcClient.sql(genresSql)
                .query((rs, rowNum) -> new StockItemDto(
                        rs.getLong("id"),
                        rs.getString("name"),
                        rs.getLong("stock")
                ))
                .list();

        String booksSql = """
                SELECT b.id AS id, b.title AS name, b.stock AS stock
                FROM books b
                WHERE b.deleted_at IS NULL
                ORDER BY b.stock DESC, b.title ASC
                """;

        List<StockItemDto> byBooks = jdbcClient.sql(booksSql)
                .query((rs, rowNum) -> new StockItemDto(
                        rs.getLong("id"),
                        rs.getString("name"),
                        rs.getLong("stock")
                ))
                .list();

        return new StockAnalyticsResponse(byGenres, byBooks);
    }

    public RatingAnalyticsResponse getRatingAnalytics(LocalDate from, LocalDate to) {
        PeriodDates period = validateAndResolvePeriod(from, to);

        String genresSql = """
                SELECT g.id AS id, g.name AS name,
                       ROUND(AVG(r.value)::numeric, 2) AS average_rating,
                       COUNT(r.id) AS ratings_count
                FROM ratings r
                JOIN books b ON r.book_id = b.id
                JOIN genres g ON b.genre_id = g.id
                WHERE r.created_at <= :toEnd
                GROUP BY g.id, g.name
                ORDER BY average_rating DESC NULLS LAST, ratings_count DESC, g.name ASC
                """;

        List<RatingChartItemDto> byGenres = jdbcClient.sql(genresSql)
                .param("toEnd", period.toEnd())
                .query((rs, rowNum) -> {
                    BigDecimal avg = rs.getBigDecimal("average_rating");
                    Double avgValue = avg != null ? avg.setScale(2, RoundingMode.HALF_UP).doubleValue() : null;
                    return new RatingChartItemDto(
                            rs.getLong("id"),
                            rs.getString("name"),
                            avgValue,
                            rs.getLong("ratings_count")
                    );
                })
                .list();

        String booksSql = """
                SELECT b.id AS id, b.title AS name,
                       ROUND(AVG(r.value)::numeric, 2) AS average_rating,
                       COUNT(r.id) AS ratings_count
                FROM ratings r
                JOIN books b ON r.book_id = b.id
                WHERE r.created_at <= :toEnd
                GROUP BY b.id, b.title
                ORDER BY average_rating DESC NULLS LAST, ratings_count DESC, b.title ASC
                """;

        List<RatingChartItemDto> byBooks = jdbcClient.sql(booksSql)
                .param("toEnd", period.toEnd())
                .query((rs, rowNum) -> {
                    BigDecimal avg = rs.getBigDecimal("average_rating");
                    Double avgValue = avg != null ? avg.setScale(2, RoundingMode.HALF_UP).doubleValue() : null;
                    return new RatingChartItemDto(
                            rs.getLong("id"),
                            rs.getString("name"),
                            avgValue,
                            rs.getLong("ratings_count")
                    );
                })
                .list();

        return new RatingAnalyticsResponse(byGenres, byBooks);
    }

    private PeriodDates validateAndResolvePeriod(LocalDate from, LocalDate to) {
        if (to == null && from == null) {
            to = LocalDate.now();
            from = to.minusMonths(3);
        } else if (to == null) {
            to = LocalDate.now();
        } else if (from == null) {
            from = to.minusMonths(3);
        }

        if (to.isBefore(from) || to.isAfter(LocalDate.now())) {
            throw new BusinessRuleException("Некоректний період аналітики: кінцева дата не може бути ранішою за початкову або перебувати у майбутньому");
        }

        OffsetDateTime fromStart = from.atStartOfDay().atOffset(ZoneOffset.UTC);
        OffsetDateTime toEnd = to.atTime(LocalTime.MAX).atOffset(ZoneOffset.UTC);

        return new PeriodDates(from, to, fromStart, toEnd);
    }

    private record PeriodDates(LocalDate from, LocalDate to, OffsetDateTime fromStart, OffsetDateTime toEnd) {}
}
