package com.thanh.auction_server.service.auction;

import com.thanh.auction_server.constants.ErrorMessage;
import com.thanh.auction_server.dto.request.BidRequest;
import com.thanh.auction_server.entity.Bid;
import com.thanh.auction_server.entity.User;
import com.thanh.auction_server.exception.DataConflictException;
import com.thanh.auction_server.exception.ResourceNotFoundException;
import com.thanh.auction_server.mapper.BidMapper;
import com.thanh.auction_server.repository.AuctionSessionRepository;
import com.thanh.auction_server.repository.BidRepository;
import com.thanh.auction_server.repository.UserRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
@Service
public class BidService {
    BidRepository bidRepository;
    BidMapper bidMapper;
    AuctionSessionRepository auctionSessionRepository;
    UserRepository userRepository;

    private BigDecimal calculateIncrement(BigDecimal currentPrice) {
        if (currentPrice == null || currentPrice.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.valueOf(10000);
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

    public void placeBid(Long auctionSessionId,BidRequest request) {
        var session = auctionSessionRepository.findById(auctionSessionId)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorMessage.AUCTION_SESSION_NOT_FOUND));

        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        var bidder = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorMessage.USER_NOT_FOUND));

//        if(session.getProduct().getSeller().getId().equals(bidder.getId())){
//            throw new DataConflictException("Seller cannot place bid on their own product.");
//        }
        BigDecimal currentPrice = session.getCurrentPrice();
        BigDecimal minimumNextBid = calculateMinimumNextBid(currentPrice);
        BigDecimal newMaxBid = request.getAmount();

        if (newMaxBid.compareTo(minimumNextBid) < 0) {
            throw new DataConflictException("Minimum bid is" + minimumNextBid + " VND");
        }
        // PROXY BIDDING LOGIC
        User currentHighestBidder = session.getHighestBidder();
        BigDecimal currentHighestMaxBid =
                session.getHighestMaxBid() != null ? session.getHighestMaxBid() : BigDecimal.ZERO;
        // TH1: User hiện tại là người dẫn đầu -> đặt giá cao hơn giá đặt tối đa hiện tại của họ
        if (currentHighestBidder != null && currentHighestBidder.getId().equals(bidder.getId())) {
            if (newMaxBid.compareTo(currentHighestMaxBid) > 0) {
                session.setHighestMaxBid(newMaxBid);
                log.info("User {} (current winner) updated their max bid to {} VND for session {}", bidder.getUsername(), newMaxBid, auctionSessionId);
            } else {
                throw new DataConflictException("Giá đặt mới phải cao hơn giá đặt tối đa hiện tại của bạn (" + currentHighestMaxBid + " VND)");
            }
        // TH2: Other user bid or bid lan dau
        } else {
            if(newMaxBid.compareTo(currentHighestMaxBid)>0){
                session.setHighestBidder(bidder);
                session.setHighestMaxBid(newMaxBid);

                // Tính giá hiển thị mới: Max Bid cũ + Bước giá tại mức Max Bid cũ
                BigDecimal incrementAtOldMax = calculateIncrement(currentHighestMaxBid);
                BigDecimal newCurrentPrice = currentHighestMaxBid.add(incrementAtOldMax);
                if (newCurrentPrice.compareTo(newMaxBid) > 0) {
                    newCurrentPrice = newMaxBid;
                }
                session.setCurrentPrice(newCurrentPrice);
                log.info("New highest bidder for session {}: User {}. New Max Bid: {}. New Current Price: {} VND",
                        auctionSessionId, bidder.getUsername(), newMaxBid, newCurrentPrice);
                // ... (Gửi thông báo cho người thắng cũ) ...
            } else {
                // Không đủ để thắng, chỉ đẩy giá người hiện tại lên
                // Giá hiển thị mới: Max Bid mới + Bước giá tại mức Max Bid mới
                BigDecimal incrementAtNewBid = calculateIncrement(newMaxBid);
                BigDecimal newCurrentPrice = newMaxBid.add(incrementAtNewBid);

                if (newCurrentPrice.compareTo(currentHighestMaxBid) > 0) {
                    newCurrentPrice = currentHighestMaxBid;
                }
                session.setCurrentPrice(newCurrentPrice);
                log.info("Bidder {} placed bid {} VND, not enough to win session {}. Current Price updated to {} VND",
                        bidder.getUsername(), newMaxBid, auctionSessionId, newCurrentPrice);
            }
        }
        // --- KIỂM TRA GIÁ SÀN (RESERVE PRICE) ---
        // Chỉ kiểm tra khi có người thắng mới hoặc đây là bid đầu tiên
        BigDecimal reservePrice = session.getReservePrice();
        if (reservePrice != null && session.getCurrentPrice().compareTo(reservePrice) >= 0) {
            // Nếu giá hiện tại đạt hoặc vượt giá sàn
            // Logic bổ sung nếu cần thông báo "Giá sàn đã đạt"
            log.debug("Reserve price met for session {}", auctionSessionId);
        }

        // --- VÔ HIỆU HÓA BUY IT NOW (NẾU CÓ BID ĐẦU TIÊN) ---
        if (session.getBuyNowPrice() != null && session.getBids().isEmpty()) {
            log.info("First bid placed for session {}. Disabling Buy It Now price.", auctionSessionId);
            session.setBuyNowPrice(null); // Vô hiệu hóa giá mua ngay
        }

        // --- LƯU THAY ĐỔI ---
        // 1. Lưu bản ghi Bid
        LocalDateTime now = LocalDateTime.now();
        Bid newBid = bidMapper.toBid(request);
        newBid.setUser(bidder);
        newBid.setAuctionSession(session);
        newBid.setBidTime(now);
        // newBid.setPriceWhenBid(session.getCurrentPrice()); // Lưu giá hiện tại nếu cần
        bidRepository.save(newBid);

        // 2. Lưu AuctionSession đã cập nhật
        session.setUpdatedAt(now);
        auctionSessionRepository.save(session);

        // --- GỬI THÔNG BÁO (WebSocket) ---
        // TODO: Gửi update về currentPrice, highestBidder cho các client đang xem phiên này
        // Gửi thông báo "Bạn đã bị vượt qua" cho previousHighestBidder (nếu isNewHighestBidder)

        log.info("Bid placed successfully by User {} for session {}. Amount (Max Bid): {}", bidder.getUsername(), auctionSessionId, newMaxBid);

        // return bidMapper.toBidResponse(newBid); // Trả về bid vừa tạo nếu cần

    }
}
