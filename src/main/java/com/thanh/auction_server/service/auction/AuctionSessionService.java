package com.thanh.auction_server.service.auction;

import com.thanh.auction_server.constants.*;
import com.thanh.auction_server.dto.request.AuctionSessionRequest;
import com.thanh.auction_server.dto.response.AuctionSessionResponse;
import com.thanh.auction_server.dto.response.CreateAuctionSessionResponse;
import com.thanh.auction_server.dto.response.PageResponse;
import com.thanh.auction_server.entity.AuctionSession;
import com.thanh.auction_server.entity.Invoice;
import com.thanh.auction_server.entity.User;
import com.thanh.auction_server.exception.DataConflictException;
import com.thanh.auction_server.exception.ResourceNotFoundException;
import com.thanh.auction_server.mapper.AuctionSessionMapper;
import com.thanh.auction_server.repository.AuctionSessionRepository;
import com.thanh.auction_server.repository.InvoiceRepository;
import com.thanh.auction_server.repository.ProductRepository;
import com.thanh.auction_server.service.invoice.InvoiceService;
import com.thanh.auction_server.service.payment.PaymentService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.context.SecurityContextHolder;
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
    InvoiceRepository invoiceRepository;
    NotificationService notificationService;
    InvoiceService invoiceService;
    PaymentService paymentService;

    @Transactional
    public CreateAuctionSessionResponse createAuctionSession(AuctionSessionRequest request, HttpServletRequest httpRequest) {
        var product = productRepository.findByIdAndIsActiveTrue(request.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException(ErrorMessage.PRODUCT_NOT_FOUND));
        if (auctionSessionRepository.existsByProduct_Id(request.getProductId())) {
            throw new DataConflictException(ErrorMessage.AUCTION_SESSION_ALREADY_EXISTS_FOR_PRODUCT);
        }
        if (request.getEndTime().isBefore(request.getStartTime()) || request.getEndTime().isEqual(request.getStartTime())) {
            throw new DataConflictException("End time must be after start time.");
        }
        if (request.getStartTime().isBefore(LocalDateTime.now())) {
            throw new DataConflictException("Start time must be in the future.");
        }
        if (request.getBuyNowPrice() != null && request.getBuyNowPrice().compareTo(product.getStartPrice()) <= 0) {
            throw new DataConflictException("Buy now price must be greater than starting price.");
        }
        if (request.getBuyNowPrice() != null && request.getBuyNowPrice().compareTo(request.getReservePrice()) <= 0) {
            throw new DataConflictException("Buy now price must be greater than or equal to reserve price.");
        }
        if (!ProductStatus.ACTIVE.equals(product.getStatus())) {
            throw new DataConflictException("Sản phẩm chưa được duyệt hoặc bị từ chối. Vui lòng chờ Admin kiểm duyệt trước khi tạo phiên đấu giá.");
        }
        AuctionSession auctionSession = auctionSessionMapper.toAuctionSession(request);
        auctionSession.setProduct(product);
        auctionSession.setStartPrice(product.getStartPrice());
        auctionSession.setCurrentPrice(product.getStartPrice());
        auctionSession.setStatus(AuctionStatus.SCHEDULED);
        auctionSession.setCreatedAt(LocalDateTime.now());

        auctionSession.setReservePrice(request.getReservePrice());
        BigDecimal reservePrice = request.getReservePrice() == null ? BigDecimal.ZERO : request.getReservePrice();
        // TÍNH PHÍ GIA SAN & TRẢ LINK THANH TOÁN
        if (reservePrice.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal feeAmount = reservePrice.multiply(BigDecimal.valueOf(0.05));
            //Lưu Session (Status: WAITING_PAYMENT)
            auctionSession.setStatus(AuctionStatus.WAITING_PAYMENT);
            auctionSessionRepository.save(auctionSession);
            Invoice feeInvoice = Invoice.builder()
                    .user(product.getSeller())
                    .auctionSession(auctionSession)
                    .product(product)
                    .finalPrice(feeAmount)
                    .type(InvoiceType.LISTING_FEE)
                    .status(InvoiceStatus.PENDING)
                    .createdAt(LocalDateTime.now())
                    .dueDate(LocalDateTime.now().plusMinutes(15))
                    .shippingAddress("Phí dịch giá sàn 5%")
                    .recipientName(product.getSeller().getUsername())
                    .recipientPhone(product.getSeller().getPhoneNumber())
                    .build();
            invoiceRepository.save(feeInvoice);

            String paymentUrl = paymentService.createVnPayPayment(httpRequest, feeInvoice.getId(), null);
            //Trả về Link để Frontend redirect
            return CreateAuctionSessionResponse.builder()
                    .message("Vui lòng thanh toán phí gía sàn: " + feeAmount + "VND")
                    .paymentUrl(paymentUrl)
                    .build();
        } else {
            // No reserve price
            if (request.getStartTime().isAfter(LocalDateTime.now())) {
                auctionSession.setStatus(AuctionStatus.SCHEDULED);
            } else {
                auctionSession.setStatus(AuctionStatus.ACTIVE);
            }
            auctionSessionRepository.save(auctionSession);
            return CreateAuctionSessionResponse.builder()
                    .message("Create auction session successfully.")
                    .sessionDetails(auctionSessionMapper.toAuctionSessionResponse(auctionSession))
                    .build();
        }
    }

    public PageResponse<AuctionSessionResponse> getAllAuctionSessions(AuctionStatus status, int page, int size) {
        Pageable pageable = PageRequest.of(page - 1, size);
        Specification<AuctionSession> spec = (root, query, cb) -> {
            if (status != null) {
                return cb.equal(root.get("status"), status);
            }
            return cb.conjunction();
        };
        Page<AuctionSession> sessionPage = auctionSessionRepository.findAll(spec, pageable);
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

    public List<AuctionSessionResponse> getTopPopularSessions() {
        Pageable limit = PageRequest.of(0, 5);
        List<AuctionSession> topSessions = auctionSessionRepository.findTopPopularSessions(AuctionStatus.ACTIVE, limit);
        return topSessions.stream()
                .map(auctionSessionMapper::toAuctionSessionResponse)
                .toList();
    }

    public PageResponse<AuctionSessionResponse> getMyJoinedSessions(AuctionStatus status, int page, int size) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        Pageable pageable = PageRequest.of(page - 1, size);
        Page<AuctionSession> sessionPage = auctionSessionRepository.findSessionsByBidder(username, status, pageable);
        return PageResponse.<AuctionSessionResponse>builder()
                .currentPage(page)
                .totalPages(sessionPage.getTotalPages())
                .pageSize(sessionPage.getSize())
                .totalElements(sessionPage.getTotalElements())
                .data((sessionPage.getContent().stream()
                        .map(auctionSessionMapper::toAuctionSessionResponse)
                        .toList()))
                .build();
    }

    public PageResponse<AuctionSessionResponse> getMyAuctionSessions(AuctionStatus status, int page, int size) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        Pageable pageable = PageRequest.of(page - 1, size);
        Page<AuctionSession> sessionPage = auctionSessionRepository.findBySellerUsernameAndStatus(username, status, pageable);
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

    public PageResponse<AuctionSessionResponse> getAllAuctionActiveDesc(int page, int size) {
        Pageable pageable = PageRequest.of(page - 1, size);
        Page<AuctionSession> sessionPage =
                auctionSessionRepository.findByStatusOrderByCreatedAtDesc(AuctionStatus.ACTIVE, pageable);
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

    public PageResponse<AuctionSessionResponse> getAllAuctionScheduleDesc(int page, int size) {
        Pageable pageable = PageRequest.of(page - 1, size);
        Page<AuctionSession> sessionPage =
                auctionSessionRepository.findByStatusOrderByCreatedAtDesc(AuctionStatus.SCHEDULED, pageable);
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

    public PageResponse<AuctionSessionResponse> getActiveSessionsBySeller(String sellerId, int page, int size) {
        Pageable pageable = PageRequest.of(page - 1, size);
        Page<AuctionSession> pageData = auctionSessionRepository.findActiveSessionsBySeller(sellerId, pageable);
        return PageResponse.<AuctionSessionResponse>builder()
                .currentPage(page)
                .pageSize(size)
                .totalPages(pageData.getTotalPages())
                .totalElements(pageData.getTotalElements())
                .data(pageData.getContent().stream()
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
    public List<AuctionSession> startScheduledAuctions() {
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
        return sessionsToStart;
    }

    // Hàm private để xử lý kết thúc phiên (Giai đoạn 4)
    private void determineWinnerAndSetStatus(AuctionSession session) {
        User winner = session.getHighestBidder();
        BigDecimal finalMaxBid = session.getHighestMaxBid();
        BigDecimal reservePrice = session.getReservePrice();

        // Kiểm tra điều kiện thắng: Có người bid VÀ Max Bid >= Giá sàn (nếu có)
        if (winner != null && (reservePrice == null || finalMaxBid.compareTo(reservePrice) >= 0)) {
            session.setStatus(AuctionStatus.ENDED);
            invoiceService.createInvoiceForWinner(session, winner);
        } else {
            session.setStatus(AuctionStatus.FAILED); // Không có người thắng hợp lệ
            if (reservePrice != null && finalMaxBid != null && finalMaxBid.compareTo(reservePrice) < 0) {
                // Nếu phiên thất bại VÌ không đạt giá sàn
                // invoiceService.createFeeInvoiceForSeller(session, "RESERVE_PRICE_FEE");
            }
        }
    }

    @Transactional
    public List<AuctionSession> endActiveAuctions() {
        LocalDateTime now = LocalDateTime.now();
        List<AuctionSession> sessionsToEnd = auctionSessionRepository
                .findByStatusAndEndTimeLessThanEqual(AuctionStatus.ACTIVE, now);
        for (AuctionSession session : sessionsToEnd) {
            // 3. TÁCH BIẾN ĐỂ BIẾT TRẠNG THÁI TRƯỚC VÀ SAU
            AuctionStatus statusBefore = session.getStatus();
            determineWinnerAndSetStatus(session); // Logic xác định người thắng
            session.setUpdatedAt(LocalDateTime.now());
            auctionSessionRepository.save(session);

            // 4. GỬI THÔNG BÁO SAU KHI XÁC ĐỊNH KẾT QUẢ
            User seller = session.getProduct().getSeller();
            String productName = session.getProduct().getName();
            String productLink = "/products/" + session.getProduct().getId(); // Ví dụ

            if (session.getStatus() == AuctionStatus.ENDED) {
                // Có người thắng
                User winner = session.getHighestBidder();
                // Gửi cho người thắng
                String winMsg = String.format("Chúc mừng! Bạn đã thắng phiên đấu giá '%s' với giá %,.0f VND.", productName, session.getCurrentPrice());
                String winLink = "/auctions/" + session.getId(); // Hoặc link tới trang thanh toán
                notificationService.createNotification(winner, winMsg, winLink);
                // Gửi cho người bán
                String sellerWinMsg = String.format("Sản phẩm '%s' của bạn đã được bán thành công cho %s.", productName, winner.getUsername());
                notificationService.createNotification(seller, sellerWinMsg, productLink);

            } else if (session.getStatus() == AuctionStatus.FAILED) {
                // Không có người thắng
                // Gửi cho người bán
                String sellerFailMsg = String.format("Phiên đấu giá '%s' của bạn đã kết thúc mà không có người thắng.", productName);
                notificationService.createNotification(seller, sellerFailMsg, productLink);
            }

        }
        return sessionsToEnd;
    }

}
