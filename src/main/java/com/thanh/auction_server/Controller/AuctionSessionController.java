package com.thanh.auction_server.Controller;

import com.thanh.auction_server.constants.AuctionStatus;
import com.thanh.auction_server.dto.request.AdminUpdateSessionRequest;
import com.thanh.auction_server.dto.request.AuctionSessionAdminSearchRequest;
import com.thanh.auction_server.dto.request.AuctionSessionRequest;
import com.thanh.auction_server.dto.response.*;
import com.thanh.auction_server.service.auction.AuctionSessionService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RequiredArgsConstructor
@RestController
@RequestMapping("/auction-sessions")
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AuctionSessionController {
    AuctionSessionService auctionSessionService;

    @PostMapping
    public ResponseEntity<CreateAuctionSessionResponse> createAuctionSession(
            @RequestBody @Valid AuctionSessionRequest request, HttpServletRequest httpRequest) {
        return ResponseEntity.ok(auctionSessionService.createAuctionSession(request, httpRequest));
    }

    @GetMapping("/my-joined")
    public ResponseEntity<PageResponse<AuctionSessionResponse>> getMyJoinedSessions(
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "size", defaultValue = "10") int size,
            @RequestParam(value = "status", required = false) AuctionStatus status) {
        PageResponse<AuctionSessionResponse> response = auctionSessionService.getMyJoinedSessions(status, page, size);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/seller/{sellerId}/active")
    public ResponseEntity<PageResponse<AuctionSessionResponse>> getSellerActiveSessions(
            @PathVariable String sellerId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(auctionSessionService.getActiveSessionsBySeller(sellerId, page, size));
    }

    @GetMapping
    public ResponseEntity<PageResponse<AuctionSessionResponse>> getAuctionSessions(
            @RequestParam(value = "status", required = false) AuctionStatus status,
            @RequestParam(value = "page", required = false, defaultValue = "1") int page,
            @RequestParam(value = "size", required = false, defaultValue = "10") int size) {
        PageResponse<AuctionSessionResponse> response = auctionSessionService.getAllAuctionSessions(status, page, size);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/top-popular")
    public ResponseEntity<List<AuctionSessionResponse>> getTopPopularSessions() {
        return ResponseEntity.ok(auctionSessionService.getTopPopularSessions());
    }

    @GetMapping("/my-sessions")
    public ResponseEntity<PageResponse<AuctionSessionResponse>> getMyAuctionSessions(
            @RequestParam(value = "status", required = false) AuctionStatus status,
            @RequestParam(value = "page", required = false, defaultValue = "1") int page,
            @RequestParam(value = "size", required = false, defaultValue = "10") int size) {

        PageResponse<AuctionSessionResponse> response = auctionSessionService.getMyAuctionSessions(status, page, size);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<AuctionSessionResponse> getAuctionSessionById(@PathVariable Long id) {
        AuctionSessionResponse response = auctionSessionService.getAuctionSessionById(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/active-desc")
    public ResponseEntity<PageResponse<AuctionSessionResponse>> getActiveAuctionSessionsDesc(
            @RequestParam(value = "page", required = false, defaultValue = "1") int page,
            @RequestParam(value = "size", required = false, defaultValue = "10") int size) {
        PageResponse<AuctionSessionResponse> response =
                auctionSessionService.getAllAuctionActiveDesc(page, size);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/schedule-desc")
    public ResponseEntity<PageResponse<AuctionSessionResponse>> getScheduleAuctionSessionsDesc(
            @RequestParam(value = "page", required = false, defaultValue = "1") int page,
            @RequestParam(value = "size", required = false, defaultValue = "10") int size) {
        PageResponse<AuctionSessionResponse> response =
                auctionSessionService.getAllAuctionScheduleDesc(page, size);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/buy-now")
    public ResponseEntity<InvoiceResponse> buyNow(@PathVariable Long id) {
        return ResponseEntity.ok(auctionSessionService.buyNow(id));
    }

//    ============= Admin =============

    @GetMapping("/admin/search")
    public ResponseEntity<PageResponse<AdminAuctionSessionResponse>> getAllSessionsForAdmin(
            @ModelAttribute AuctionSessionAdminSearchRequest request,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ResponseEntity.ok(auctionSessionService.getAllAuctionSessionsForAdmin(request, page, size));
    }

    @PutMapping("/admin/{id}")
    public ResponseEntity<AdminAuctionSessionResponse> updateSessionForAdmin(
            @PathVariable Long id,
            @RequestBody AdminUpdateSessionRequest request
    ) {
        return ResponseEntity.ok(auctionSessionService.updateAuctionSessionForAdmin(id, request));
    }
}
