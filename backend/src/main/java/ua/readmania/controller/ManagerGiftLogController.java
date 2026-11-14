package ua.readmania.controller;

import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ua.readmania.dto.manager.gift.GiftLogPageResponse;
import ua.readmania.service.gift.GiftLogService;

@RestController
@RequestMapping("/api/manager/gift-log")
public class ManagerGiftLogController {

    private final GiftLogService giftLogService;

    public ManagerGiftLogController(GiftLogService giftLogService) {
        this.giftLogService = giftLogService;
    }

    @GetMapping
    public ResponseEntity<GiftLogPageResponse> getGiftLog(
            @RequestParam(required = false) Long institutionId,
            @RequestParam(required = false) String institutionName,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo) {
        GiftLogPageResponse response = giftLogService.getGiftLog(institutionId, institutionName, dateFrom, dateTo);
        return ResponseEntity.ok(response);
    }
}
