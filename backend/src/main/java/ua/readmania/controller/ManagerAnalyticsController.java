package ua.readmania.controller;

import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ua.readmania.dto.manager.analytics.DemandAnalyticsResponse;
import ua.readmania.dto.manager.analytics.RatingAnalyticsResponse;
import ua.readmania.dto.manager.analytics.StockAnalyticsResponse;
import ua.readmania.service.analytics.AnalyticsService;

@RestController
@RequestMapping("/api/manager/analytics")
public class ManagerAnalyticsController {

    private final AnalyticsService analyticsService;

    public ManagerAnalyticsController(AnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    @GetMapping("/demand")
    public ResponseEntity<DemandAnalyticsResponse> getDemandAnalytics(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        DemandAnalyticsResponse response = analyticsService.getDemandAnalytics(from, to);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/stock")
    public ResponseEntity<StockAnalyticsResponse> getStockAnalytics() {
        StockAnalyticsResponse response = analyticsService.getStockAnalytics();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/ratings")
    public ResponseEntity<RatingAnalyticsResponse> getRatingAnalytics(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        RatingAnalyticsResponse response = analyticsService.getRatingAnalytics(from, to);
        return ResponseEntity.ok(response);
    }
}
