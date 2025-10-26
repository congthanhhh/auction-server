package com.thanh.auction_server.service.auction;

import com.thanh.auction_server.constants.AuctionStatus;
import com.thanh.auction_server.constants.ErrorMessage;
import com.thanh.auction_server.dto.request.AuctionSessionRequest;
import com.thanh.auction_server.dto.response.AuctionSessionResponse;
import com.thanh.auction_server.dto.response.PageResponse;
import com.thanh.auction_server.entity.AuctionSession;
import com.thanh.auction_server.entity.User;
import com.thanh.auction_server.exception.DataConflictException;
import com.thanh.auction_server.exception.ResourceNotFoundException;
import com.thanh.auction_server.mapper.AuctionSessionMapper;
import com.thanh.auction_server.repository.AuctionSessionRepository;
import com.thanh.auction_server.repository.ProductRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
@Service
public class AuctionSessionService {
    AuctionSessionRepository auctionSessionRepository;
    ProductRepository productRepository;
    AuctionSessionMapper auctionSessionMapper;

    @Transactional
    public AuctionSessionResponse createAuctionSession(AuctionSessionRequest request) {
        var product = productRepository.findByIdAndIsActiveTrue(request.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException(ErrorMessage.PRODUCT_NOT_FOUND));
        if (auctionSessionRepository.existsByProduct_Id(request.getProductId())) {
            throw new DataConflictException(ErrorMessage.AUCTION_SESSION_ALREADY_EXISTS_FOR_PRODUCT);
        }

        if(request.getEndTime().isBefore(request.getStartTime())||request.getEndTime().isEqual(request.getStartTime())){
            throw new DataConflictException("End time must be after start time.");
        }
        if (request.getStartTime().isBefore(LocalDateTime.now())) {
            throw new DataConflictException("Start time must be in the future.");
        }
        if(request.getBuyNowPrice() != null && request.getBuyNowPrice().compareTo(product.getStartPrice())<=0){
            throw new DataConflictException("Buy now price must be greater than starting price.");
        }
        if(request.getReservePrice() != null && request.getReservePrice().compareTo(product.getStartPrice())<=0){
            throw new DataConflictException("Reserve price must be greater than or equal to starting price.");
        }
        if(request.getBuyNowPrice() != null && request.getBuyNowPrice().compareTo(request.getReservePrice())<=0){
            throw new DataConflictException("Buy now price must be greater than or equal to reserve price.");
        }

        var session = auctionSessionMapper.toAuctionSession(request);
        session.setProduct(product);
        session.setStartPrice(product.getStartPrice());
        session.setCurrentPrice(product.getStartPrice());
        session.setStatus(AuctionStatus.SCHEDULED);
        session.setCreatedAt(LocalDateTime.now());

        AuctionSession savedSession = auctionSessionRepository.save(session);
        return auctionSessionMapper.toAuctionSessionResponse(savedSession);
    }

    public PageResponse<AuctionSessionResponse> getAllAuctionSessions(int page, int size) {
        Pageable pageable = PageRequest.of(page - 1, size);
        Page<AuctionSession> sessionPage = auctionSessionRepository.findAll(pageable);
        return PageResponse.<AuctionSessionResponse>builder()
                .currentPage(page)
                .totalPages(sessionPage.getTotalPages())
                .pageSize(sessionPage.getSize())
                .totalElements(sessionPage.getTotalElements())
                .data(sessionPage.getContent().stream()
                        .map(auctionSessionMapper::toAuctionSessionResponse)
                        .toList())
                .build();
    }


    // Lấy chi tiết một phiên đấu giá
    public AuctionSessionResponse getAuctionSessionById(Long id) {
        log.info("Fetching auction session with ID: {}", id);
        AuctionSession session = auctionSessionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorMessage.AUCTION_SESSION_NOT_FOUND + id));
        return auctionSessionMapper.toAuctionSessionResponse(session);
    }

    // Các phương thức cập nhật trạng thái (start, end) sẽ được gọi bởi Scheduled Job sau này
    @Transactional
    public void startScheduledAuctions() {
        LocalDateTime now = LocalDateTime.now();
        List<AuctionSession> sessionsToStart = auctionSessionRepository
                .findByStatusAndStartTimeLessThanEqual(AuctionStatus.SCHEDULED, now);
        if (!sessionsToStart.isEmpty()) {
            log.info("Found {} auction sessions to start.", sessionsToStart.size());
            for (AuctionSession session : sessionsToStart) {
                session.setStatus(AuctionStatus.ACTIVE);
                session.setUpdatedAt(now);
                auctionSessionRepository.save(session);
                // Gửi thông báo (WebSocket/Email) cho người quan tâm (nếu có)
            }
        }
    }

    @Transactional
    public void endActiveAuctions() {
        LocalDateTime now = LocalDateTime.now();
        List<AuctionSession> sessionsToEnd = auctionSessionRepository
                .findByStatusAndEndTimeLessThanEqual(AuctionStatus.ACTIVE, now);
        if (!sessionsToEnd.isEmpty()) {
            log.info("Found {} auction sessions to end.", sessionsToEnd.size());
            for (AuctionSession session : sessionsToEnd) {
                determineWinnerAndSetStatus(session); // Logic xác định người thắng
                session.setUpdatedAt(now);
                auctionSessionRepository.save(session);
                // Gửi thông báo kết thúc, thông báo người thắng/thua
            }
        }
    }

    // Hàm private để xử lý kết thúc phiên (Giai đoạn 4)
    private void determineWinnerAndSetStatus(AuctionSession session) {
        User winner = session.getHighestBidder();
        BigDecimal finalMaxBid = session.getHighestMaxBid();
        BigDecimal reservePrice = session.getReservePrice();

        // Kiểm tra điều kiện thắng: Có người bid VÀ Max Bid >= Giá sàn (nếu có)
        if (winner != null && (reservePrice == null || finalMaxBid.compareTo(reservePrice) >= 0)) {
            session.setStatus(AuctionStatus.PENDING_PAYMENT);
            log.info("Auction session ID {} ended. Winner: {}. Final Price: {}", session.getId(), winner.getUsername(), session.getCurrentPrice());
            // TODO: Tạo hóa đơn/order
            // TODO: Gửi thông báo cho người thắng và người bán
        } else {
            session.setStatus(AuctionStatus.FAILED); // Không có người thắng hợp lệ
            log.info("Auction session ID {} ended with no valid winner (No bids or reserve not met).", session.getId());
            // TODO: Gửi thông báo cho người bán
        }
    }
}
