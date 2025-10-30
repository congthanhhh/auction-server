package com.thanh.auction_server.Controller;

import com.thanh.auction_server.dto.request.BidRequest;
import com.thanh.auction_server.dto.response.BidResponse;
import com.thanh.auction_server.dto.response.PageResponse;
import com.thanh.auction_server.service.auction.BidService;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RequiredArgsConstructor
@RestController
@RequestMapping("/auction-sessions/{sessionId}/bids")
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class BidController {
    BidService bidService;

    @PostMapping
    public ResponseEntity<BidResponse> placeBid(@PathVariable Long sessionId,
                                                @RequestBody @Valid BidRequest request) {
        return ResponseEntity.ok(bidService.placeBid(sessionId, request));
    }

    @GetMapping
    public ResponseEntity<PageResponse<BidResponse>> getBidHistory(
            @PathVariable Long sessionId,
            @RequestParam(value = "page", required = false, defaultValue = "1") int page,
            @RequestParam(value = "size", required = false, defaultValue = "10") int size) {
        PageResponse<BidResponse> history = bidService.getBidHistory(sessionId, page, size);
        return ResponseEntity.ok(history);
    }
}
