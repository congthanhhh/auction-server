package com.thanh.auction_server.Controller;

import com.thanh.auction_server.dto.request.FeedbackRequest;
import com.thanh.auction_server.dto.response.MessageResponse;
import com.thanh.auction_server.service.auction.FeedbackService;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RequiredArgsConstructor
@RestController
@RequestMapping("/feedback")
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class FeedBackController {
    FeedbackService feedbackService;

    @PostMapping("/invoice/{invoiceId}")
    public ResponseEntity<MessageResponse> createFeedback(
            @PathVariable Long invoiceId,
            @RequestBody @Valid FeedbackRequest request) {
        return ResponseEntity.ok(feedbackService.createFeedback(invoiceId, request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<String> updateFeedback(
            @PathVariable Long id,
            @RequestBody @Valid FeedbackRequest request) {
        return ResponseEntity.ok(feedbackService.updateFeedback(id, request));
    }
}
