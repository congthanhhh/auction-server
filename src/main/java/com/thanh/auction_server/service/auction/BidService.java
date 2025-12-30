package com.thanh.auction_server.service.auction;

import com.thanh.auction_server.constants.AuctionStatus;
import com.thanh.auction_server.constants.ErrorMessage;
import com.thanh.auction_server.dto.request.BidRequest;
import com.thanh.auction_server.dto.response.BidResponse;
import com.thanh.auction_server.dto.response.PageResponse;
import com.thanh.auction_server.dto.response.SimpleUserResponse;
import com.thanh.auction_server.entity.Bid;
import com.thanh.auction_server.entity.Product;
import com.thanh.auction_server.entity.User;
import com.thanh.auction_server.exception.DataConflictException;
import com.thanh.auction_server.exception.ResourceNotFoundException;
import com.thanh.auction_server.exception.UnauthorizedException;
import com.thanh.auction_server.mapper.BidMapper;
import com.thanh.auction_server.mapper.UserMapper;
import com.thanh.auction_server.repository.AuctionSessionRepository;
import com.thanh.auction_server.repository.BidRepository;
import com.thanh.auction_server.repository.UserRepository;
import com.thanh.auction_server.service.utils.SocketIOService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
@Service
public class BidService {
    BidRepository bidRepository;
    BidMapper bidMapper;
    AuctionSessionRepository auctionSessionRepository;
    UserRepository userRepository;
    UserMapper userMapper;
    NotificationService notificationService;
    SocketIOService socketIOService;

    private BigDecimal calculateIncrement(BigDecimal currentPrice) {
        if (currentPrice == null || currentPrice.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.valueOf(5000);
        }

        double price = currentPrice.doubleValue();
        if (price < 50000) {                 // Dưới 50,000đ
            return BigDecimal.valueOf(5000);   // Bước giá: 5,000đ
        } else if (price < 200000) {         // Từ 50,000đ đến dưới 200,000đ
            return BigDecimal.valueOf(10000);  // Bước giá: 10,000đ
        } else if (price < 500000) {         // Từ 200,000đ đến dưới 500,000đ
            return BigDecimal.valueOf(20000);  // Bước giá: 20,000đ
        } else if (price < 1000000) {        // Từ 500,000đ đến dưới 1,000,000đ
            return BigDecimal.valueOf(50000);  // Bước giá: 50,000đ
        } else if (price < 5000000) {        // Từ 1,000,000đ đến dưới 5,000,000đ
            return BigDecimal.valueOf(100000); // Bước giá: 100,000đ
        } else if (price < 10000000) {       // Từ 5,000,000đ đến dưới 10,000,000đ
            return BigDecimal.valueOf(200000); // Bước giá: 200,000đ
        } else if (price < 50000000) {       // Từ 10,000,000đ đến dưới 50,000,000đ
            return BigDecimal.valueOf(500000); // Bước giá: 500,000đ
        } else {                             // Từ 50,000,000đ trở lên
            return BigDecimal.valueOf(1000000);// Bước giá: 1,000,000đ
        }
    }

    private BigDecimal calculateMinimumNextBid(BigDecimal currentPrice) {
        return currentPrice.add(calculateIncrement(currentPrice));
    }

    @Transactional
    public BidResponse placeBid(Long auctionSessionId, BidRequest request) {
        LocalDateTime now = LocalDateTime.now();
        var session = auctionSessionRepository.findByIdWithLock(auctionSessionId)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorMessage.AUCTION_SESSION_NOT_FOUND));
        Product product = session.getProduct();

        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        var bidder = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorMessage.USER_NOT_FOUND));

        final int MAX_STRIKES_ALLOWED = 2;
        if (bidder.getStrikeCount() != null && bidder.getStrikeCount() >= MAX_STRIKES_ALLOWED) {
            log.warn("User {} (Strikes: {}) bị chặn đặt giá.", bidder.getUsername(), bidder.getStrikeCount());
            throw new UnauthorizedException("Tài khoản của bạn đã bị cấm đấu giá do vi phạm không thanh toán.");
        }
        if (session.getStatus() != AuctionStatus.ACTIVE) {
            throw new ResourceNotFoundException("Phiên đấu giá không hoạt động.");
        }
        if (now.isAfter(session.getEndTime())) {
            throw new ResourceNotFoundException("Phiên đấu giá đã kết thúc.");
        }
        if (product.getSeller().getId().equals(bidder.getId())) {
            throw new DataConflictException("Người bán không được tham gia đấu giá sản phẩm của mình.");
        }
        BigDecimal currentPrice = session.getCurrentPrice();
        BigDecimal minimumNextBid = calculateMinimumNextBid(currentPrice);
        BigDecimal newMaxBid = request.getAmount();

        // Check bid hợp lệ
        if (session.getHighestMaxBid() == null) {
            if (newMaxBid.compareTo(session.getStartPrice()) < 0) {
                throw new DataConflictException("Minimum bid is " + session.getStartPrice() + " VND");
            }
        } else {
            User current = session.getHighestBidder();
            if (current != null && !current.getId().equals(bidder.getId()) && newMaxBid.compareTo(minimumNextBid) < 0) {
                throw new DataConflictException("Minimum bid is " + minimumNextBid + " VND");
            }
        }
        // PROXY BIDDING LOGIC
        User currentHighestBidder = session.getHighestBidder();
        BigDecimal currentHighestMaxBid =
                session.getHighestMaxBid() != null ? session.getHighestMaxBid() : BigDecimal.ZERO;

        BigDecimal reservePrice = session.getReservePrice();
        boolean reserveMetBefore = (reservePrice != null && currentHighestMaxBid.compareTo(reservePrice) >= 0);
        boolean reserveMetNow = (reservePrice != null && newMaxBid.compareTo(reservePrice) >= 0);
        User previousHighestBidder = currentHighestBidder;
        boolean isNewHighestBidder = false;
        Bid savedBid = null;
        // 1. NGƯỜI ĐANG DẪN ĐẦU TỰ ĐẶT LẠI (UPDATE MAX BID)
        if (currentHighestBidder != null && currentHighestBidder.getId().equals(bidder.getId())) {
            if (newMaxBid.compareTo(currentHighestMaxBid) <= 0) {
                throw new DataConflictException("Giá mới phải cao hơn giá max hiện tại của bạn (" + currentHighestMaxBid + ")");
            }
            session.setHighestMaxBid(newMaxBid); // Cập nhật Max Bid
            // Tự nâng giá -> Check xem có vượt qua sàn chưa
            if (reservePrice != null && newMaxBid.compareTo(reservePrice) >= 0) {
                // Nếu giá hiện tại đang thấp hơn sàn -> Đẩy lên bằng sàn ngay
                if (session.getCurrentPrice().compareTo(reservePrice) < 0) {
                    session.setCurrentPrice(reservePrice);
                    reserveMetNow = true;
                }
            }
            savedBid = bidRepository.save(Bid.builder()
                    .amount(newMaxBid)
                    .bidTime(now)
                    .user(bidder)
                    .auctionSession(session)
                    .resultingPrice(session.getCurrentPrice())
                    .build());
            // 2. NGƯỜI KHÁC ĐẶT (User B) HOẶC LẦN ĐẦU TIÊN
        } else {
            // Trường hợp A: Thắng (Trở thành người dẫn đầu mới)
            if (newMaxBid.compareTo(currentHighestMaxBid) > 0) {
                isNewHighestBidder = true;
                session.setHighestBidder(bidder);
                session.setHighestMaxBid(newMaxBid);

                // Tính giá mới = Giá Max cũ + Bước giá (tại mức Max cũ)
                BigDecimal basePrice = currentHighestMaxBid.compareTo(BigDecimal.ZERO) > 0 ? currentHighestMaxBid : session.getStartPrice();
                BigDecimal increment = calculateIncrement(basePrice);
                BigDecimal newPrice = basePrice.add(increment);
                // Giá không vượt quá MaxBid của người mới
                if (newPrice.compareTo(newMaxBid) > 0) {
                    newPrice = newMaxBid;
                }
                // Check giá sàn cho người thắng mới
                // Nếu Max Bid mới đã qua sàn mà Giá tính toán < sàn -> Ép lên sàn
                if (reservePrice != null && newMaxBid.compareTo(reservePrice) >= 0) {
                    if (newPrice.compareTo(reservePrice) < 0) {
                        newPrice = reservePrice;
//                        reserveMetTrigger = true;
                        reserveMetNow = true;
                    }
                }
                if (currentHighestMaxBid.compareTo(BigDecimal.ZERO) == 0 && reservePrice != null && newMaxBid.compareTo(reservePrice) >= 0) {
                    newPrice = reservePrice;
                } else if (currentHighestMaxBid.compareTo(BigDecimal.ZERO) == 0) {
                    newPrice = session.getStartPrice();
                }
                session.setCurrentPrice(newPrice);
                savedBid = bidRepository.save(Bid.builder()
                        .amount(newMaxBid)
                        .bidTime(now)
                        .user(bidder)
                        .auctionSession(session)
                        .resultingPrice(session.getCurrentPrice())
                        .build());

                // B. TRƯỜNG HỢP THUA (Proxy Defense - User A vẫn giữ búa)
            } else {
                // --- BƯỚC 1: Lưu Bid của người thách đấu (User B) ---
                Bid challengerBid = bidRepository.save(Bid.builder()
                        .amount(newMaxBid)
                        .bidTime(now)
                        .user(bidder) // User B
                        .auctionSession(session)
                        .resultingPrice(newMaxBid)
                        .build());
                // Gửi socket riêng cho bid này để Client update lịch sử ngay lập tức
                socketIOService.sendMessageToRoom("session-" + auctionSessionId,
                        SocketIOService.EVENT_NEW_BID,
                        bidMapper.toBidResponse(challengerBid));

                // --- BƯỚC 2: Tính toán giá mới cho người dẫn đầu (User A) ---
                BigDecimal increment = calculateIncrement(newMaxBid);
                BigDecimal newCurrentPrice = newMaxBid.add(increment);

                // Không vượt quá Max Bid của User A
                if (newCurrentPrice.compareTo(currentHighestMaxBid) > 0) {
                    newCurrentPrice = currentHighestMaxBid;
                }

                // Check giá sàn (User A cover được sàn)
                if (reservePrice != null && currentHighestMaxBid.compareTo(reservePrice) >= 0) {
                    if (newCurrentPrice.compareTo(reservePrice) < 0) {
                        newCurrentPrice = reservePrice;
                    }
                }
                session.setCurrentPrice(newCurrentPrice);

                // --- BƯỚC 3: Lưu Auto Bid cho người dẫn đầu (User A) ---
                savedBid = bidRepository.save(Bid.builder()
                        .amount(newCurrentPrice)
                        .bidTime(now.plusNanos(1000000))
                        .user(currentHighestBidder) // User A (Quan trọng: ID là của A)
                        .auctionSession(session)
                        .resultingPrice(session.getCurrentPrice())
                        .build());
            }
        }
        // --- VÔ HIỆU HÓA BUY IT NOW (NẾU CÓ BID ĐẦU TIÊN) ---
        if (session.getBuyNowPrice() != null && previousHighestBidder == null) {
            session.setBuyNowPrice(null);
        }
        session.setUpdatedAt(now);
        auctionSessionRepository.save(session);

        // --- GỬI THÔNG BÁO (WebSocket) ---
        log.info("Broadcasting WebSocket updates for session ID: {}", auctionSessionId);
        String roomName = "session-" + auctionSessionId;
        // A. Chuẩn bị dữ liệu cho Lịch sử Bid (Sự kiện "new_bid")
        BidResponse bidResponse = bidMapper.toBidResponse(savedBid);
        bidResponse.setDisplayedAmount(savedBid.getResultingPrice());

        // B. Chuẩn bị dữ liệu cho Cập nhật giá (Sự kiện "price_update")
        SimpleUserResponse highestBidderResponse = userMapper.userToSimpleUserResponse(session.getHighestBidder());
        boolean isMet = session.getReservePrice() == null ||
                savedBid.getResultingPrice().compareTo(session.getReservePrice()) >= 0;

        Map<String, Object> priceUpdateData = Map.of(
                "currentPrice", savedBid.getResultingPrice(),
                "highestBidder", highestBidderResponse,
                "reservePriceMet", isMet
        );
        // C. Gửi 2 sự kiện đến phòng
        // Gửi thông báo có bid mới cho lịch sử
        socketIOService.sendMessageToRoom(roomName, SocketIOService.EVENT_NEW_BID, bidResponse);
        // Gửi thông báo giá mới cho UI chính
        socketIOService.sendMessageToRoom(roomName, SocketIOService.EVENT_PRICE_UPDATE, priceUpdateData);
        // ------------------------------------

        log.info("Bid placed and WebSocket events sent for session {}.", auctionSessionId);
        // Gửi thông báo qua hệ thống Notification
        sendBidNotifications(
                savedBid,
                bidder,
                previousHighestBidder,
                isNewHighestBidder,
                reserveMetNow,
                reserveMetBefore,
                product
        );
        return bidResponse;
    }

    public PageResponse<BidResponse> getBidHistory(Long auctionSessionId, int page, int size) {
        Pageable pageable = PageRequest.of(page - 1, size);
        Page<Bid> bidPage = bidRepository.findByAuctionSessionIdOrderByBidTimeDesc(auctionSessionId, pageable);
        List<BidResponse> bidResponses = bidPage.getContent()
                .stream()
                .map(bidMapper::toBidResponse)
                .collect(Collectors.toList());

        return PageResponse.<BidResponse>builder()
                .currentPage(page)
                .totalPages(bidPage.getTotalPages())
                .pageSize(bidPage.getSize())
                .totalElements(bidPage.getTotalElements())
                .data(bidResponses)
                .build();
    }

    private void sendBidNotifications(
            Bid savedBid,
            User bidder,
            User previousHighestBidder,
            boolean isNewHighestBidder,
            boolean reserveMetNow,
            boolean reserveMetBefore,
            Product product) {

        String link = "/auction/" + savedBid.getAuctionSession().getId();
        String productName = product.getName();

        if (isNewHighestBidder) {
            // 1. thông báo cho NGƯỜI BỊ VƯỢT QUA
            if (previousHighestBidder != null) {
                String outbidMsg = String.format(
                        "Bạn đã bị vượt qua trong phiên đấu giá '%s'!. Giá hiện tại: '%s'",
                        productName,
                        savedBid.getResultingPrice()
                );
                notificationService.createNotification(previousHighestBidder, outbidMsg, link);
            }

            // 2. thông báo cho NGƯỜI CHIẾN THẮNG MỚI
            String winnerMsg;
            if (reserveMetNow) {
                // 2a. Người chiến thắng MỚI và giá sàn ĐÃ được đáp ứng
                if (!reserveMetBefore) {
                    // Người chiến thắng MỚI và giá sàn ĐƯỢC đáp ứng LẦN ĐẦU TIÊN
                    winnerMsg = String.format(
                            "Chúc mừng! Bạn là người đầu tiên đạt giá sàn cho '%s' và đang dẫn đầu với mức giá: '%s'.",
                            productName, savedBid.getResultingPrice()
                    );
                } else {
                    // Giai sàn đã được đáp ứng từ trước
                    winnerMsg = String.format(
                            "Bạn đang dẫn đầu phiên đấu giá '%s', mức giá hiện tại '%s'.",
                            productName,
                            savedBid.getResultingPrice()
                    );
                }
            } else {
                // 2b. Người chiến thắng MỚI nhưng giá sàn CHƯA được đáp ứng
                winnerMsg = String.format(
                        "Bạn đang dẫn đầu phiên đấu giá '%s', nhưng giá sàn chưa được đáp ứng.",
                        productName
                );
            }
            notificationService.createNotification(bidder, winnerMsg, link);
        } else {
            // B. Bid KHÔNG thành công do max bid không đủ cao
            if (previousHighestBidder != null && !previousHighestBidder.getId().equals(bidder.getId())) {
                String notEnoughMsg = String.format(
                        "Giá bạn đặt cho '%s' chưa đủ cao. Bạn đã bị vượt qua, mức giá hiện tại: '%s'",
                        productName,
                        savedBid.getResultingPrice()
                );
                notificationService.createNotification(bidder, notEnoughMsg, link);
            }
        }
    }

}
