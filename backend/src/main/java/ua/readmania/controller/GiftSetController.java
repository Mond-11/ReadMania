package ua.readmania.controller;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ua.readmania.dto.giftset.GenerateGiftSetRequest;
import ua.readmania.dto.giftset.GiftSetResponse;
import ua.readmania.dto.giftset.ReplaceCandidatesResponse;
import ua.readmania.dto.giftset.ReplaceGiftSetBookRequest;
import ua.readmania.service.giftset.GiftSetService;

@RestController
@RequestMapping("/api/gift-sets")
public class GiftSetController {

    private final GiftSetService giftSetService;

    public GiftSetController(GiftSetService giftSetService) {
        this.giftSetService = giftSetService;
    }

    @PostMapping("/generate")
    public ResponseEntity<GiftSetResponse> generateGiftSet(@Valid @RequestBody GenerateGiftSetRequest request) {
        GiftSetResponse response = giftSetService.generateGiftSet(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/replace")
    public ResponseEntity<GiftSetResponse> replaceBook(@Valid @RequestBody ReplaceGiftSetBookRequest request) {
        GiftSetResponse response = giftSetService.replaceBook(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/replace-candidates")
    public ResponseEntity<ReplaceCandidatesResponse> getReplaceCandidates(@Valid @RequestBody ReplaceGiftSetBookRequest request) {
        ReplaceCandidatesResponse response = giftSetService.getReplaceCandidates(request);
        return ResponseEntity.ok(response);
    }
}
