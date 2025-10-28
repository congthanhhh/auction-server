package com.thanh.auction_server.Controller;

import com.thanh.auction_server.constants.AuctionStatus;
import com.thanh.auction_server.dto.request.AuctionSessionRequest;
import com.thanh.auction_server.dto.response.AuctionSessionResponse;
import com.thanh.auction_server.dto.response.PageResponse;
import com.thanh.auction_server.service.auction.AuctionSessionService;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RequiredArgsConstructor
@RestController
@RequestMapping("/auction-sessions")
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AuctionSessionController {
    AuctionSessionService auctionSessionService;

    @PostMapping
    public ResponseEntity<AuctionSessionResponse> createAuctionSession(
            @RequestBody @Valid AuctionSessionRequest request) {
        AuctionSessionResponse response = auctionSessionService.createAuctionSession(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<PageResponse<AuctionSessionResponse>> getAuctionSessions(
            @RequestParam(value = "status", required = false) AuctionStatus status,
            @RequestParam(value = "page", required = false, defaultValue = "1") int page,
            @RequestParam(value = "size", required = false, defaultValue = "10") int size) {
        PageResponse<AuctionSessionResponse> response = auctionSessionService.getAllAuctionSessions(status, page, size);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<AuctionSessionResponse> getAuctionSessionById(@PathVariable Long id) {
        AuctionSessionResponse response = auctionSessionService.getAuctionSessionById(id);
        return ResponseEntity.ok(response);
    }
}
