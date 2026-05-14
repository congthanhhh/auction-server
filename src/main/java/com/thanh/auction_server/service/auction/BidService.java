package com.thanh.auction_server.service.auction;

import com.thanh.auction_server.constants.AuctionStatus;
import com.thanh.auction_server.constants.ErrorMessage;
import com.thanh.auction_server.dto.request.BidRequest;
import com.thanh.auction_server.dto.response.BidResponse;
import com.thanh.auction_server.dto.response.PageResponse;
import com.thanh.auction_server.dto.response.SimpleUserResponse;
import com.thanh.auction_server.entity.AuctionSession;
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
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
@Service
public class BidService {
    private static final int BID_COOLDOWN_SECONDS = 10;
    private static final int MAX_STRIKES_ALLOWED = 2;
    private static final long AUTO_BID_TIME_OFFSET_NANOS = 1_000_000L;
    private static final BigDecimal ZERO = BigDecimal.ZERO;
    private static final List<BidIncrementTier> BID_INCREMENT_TIERS = List.of(
            new BidIncrementTier(new BigDecimal("50000"), new BigDecimal("5000")),
            new BidIncrementTier(new BigDecimal("200000"), new BigDecimal("10000")),
            new BidIncrementTier(new BigDecimal("500000"), new BigDecimal("20000")),
            new BidIncrementTier(new BigDecimal("1000000"), new BigDecimal("50000")),
            new BidIncrementTier(new BigDecimal("5000000"), new BigDecimal("100000")),
            new BidIncrementTier(new BigDecimal("10000000"), new BigDecimal("200000")),
            new BidIncrementTier(new BigDecimal("50000000"), new BigDecimal("500000"))
    );
    private static final BigDecimal DEFAULT_INCREMENT = new BigDecimal("1000000");

    BidRepository bidRepository;
    BidMapper bidMapper;
    AuctionSessionRepository auctionSessionRepository;
    UserRepository userRepository;
    UserMapper userMapper;
    NotificationService notificationService;
    SocketIOService socketIOService;

    public long getBidCountByProduct(Long productId) {
        return bidRepository.countByAuctionSession_Product_Id(productId);
    }

    @Transactional
    public BidResponse placeBid(Long auctionSessionId, BidRequest request) {
        LocalDateTime now = LocalDateTime.now();
        BidContext context = loadBidContext(auctionSessionId, now);
        BigDecimal newMaxBid = request.getAmount();

        validateBid(context, newMaxBid);
        BidPlacementResult result = applyProxyBid(context, newMaxBid);

        disableBuyNowAfterFirstBid(context.session(), result.previousHighestBidder());
        context.session().setUpdatedAt(now);
        auctionSessionRepository.save(context.session());

        BidResponse response = publishBidUpdates(auctionSessionId, context.session(), result);
        sendBidNotifications(
                result.displayBid(),
                context.bidder(),
                result.previousHighestBidder(),
                result.newHighestBidder(),
                result.reserveMetAfter(),
                result.reserveMetBefore(),
                context.product()
        );

        return response;
    }

    public PageResponse<BidResponse> getBidHistory(Long auctionSessionId, int page, int size) {
        Pageable pageable = PageRequest.of(page - 1, size);
        Page<Bid> bidPage = bidRepository.findByAuctionSessionIdOrderByBidTimeDesc(auctionSessionId, pageable);
        List<BidResponse> bidResponses = bidPage.getContent()
                .stream()
                .map(this::toBidResponse)
                .toList();

        return PageResponse.<BidResponse>builder()
                .currentPage(page)
                .totalPages(bidPage.getTotalPages())
                .pageSize(bidPage.getSize())
                .totalElements(bidPage.getTotalElements())
                .data(bidResponses)
                .build();
    }

    private BidContext loadBidContext(Long auctionSessionId, LocalDateTime now) {
        AuctionSession session = auctionSessionRepository.findByIdWithLock(auctionSessionId)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorMessage.AUCTION_SESSION_NOT_FOUND));
        User bidder = getAuthenticatedUser();
        return new BidContext(session, session.getProduct(), bidder, now);
    }

    private User getAuthenticatedUser() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorMessage.USER_NOT_FOUND));
    }

    private void validateBid(BidContext context, BigDecimal newMaxBid) {
        validateSessionCanAcceptBid(context.session(), context.now());
        validateBidderCanBid(context.product(), context.bidder());
        validateBidAmount(context.session(), context.bidder(), newMaxBid);
        enforceBidCooldown(context.session().getId(), context.bidder().getId(), context.now());
    }

    private void validateSessionCanAcceptBid(AuctionSession session, LocalDateTime now) {
        if (session.getStatus() != AuctionStatus.ACTIVE) {
            throw new ResourceNotFoundException("Phiên đấu giá không hoạt động.");
        }
        if (session.getEndTime() != null && now.isAfter(session.getEndTime())) {
            throw new ResourceNotFoundException("Phiên đấu giá đã kết thúc.");
        }
    }

    private void validateBidderCanBid(Product product, User bidder) {
        if (bidder.getStrikeCount() != null && bidder.getStrikeCount() >= MAX_STRIKES_ALLOWED) {
            log.warn("User {} (Strikes: {}) bị chặn đặt giá.", bidder.getUsername(), bidder.getStrikeCount());
            throw new UnauthorizedException("Tài khoản của bạn đã bị cấm đấu giá do vi phạm không thanh toán.");
        }
        if (product.getSeller().getId().equals(bidder.getId())) {
            throw new DataConflictException("Người bán không được tham gia đấu giá sản phẩm của mình.");
        }
    }

    private void validateBidAmount(AuctionSession session, User bidder, BigDecimal newMaxBid) {
        if (newMaxBid == null || newMaxBid.compareTo(ZERO) <= 0) {
            throw new DataConflictException("Số tiền đặt giá phải lớn hơn 0.");
        }

        if (!hasHighestBidder(session)) {
            BigDecimal minimumFirstBid = session.getStartPrice();
            if (newMaxBid.compareTo(minimumFirstBid) < 0) {
                throw new DataConflictException("Minimum bid is " + minimumFirstBid + " VND");
            }
            return;
        }

        if (isCurrentHighestBidder(session, bidder)) {
            return;
        }

        BigDecimal minimumNextBid = calculateMinimumNextBid(effectiveCurrentPrice(session));
        if (newMaxBid.compareTo(minimumNextBid) < 0) {
            throw new DataConflictException("Minimum bid is " + minimumNextBid + " VND");
        }
    }

    private void enforceBidCooldown(Long sessionId, String userId, LocalDateTime now) {
        bidRepository.findLastBidTimeBySessionAndUser(sessionId, userId)
                .map(lastBidTime -> lastBidTime.plusSeconds(BID_COOLDOWN_SECONDS))
                .filter(now::isBefore)
                .ifPresent(allowedNextBidTime -> {
                    long secondsLeft = Duration.between(now, allowedNextBidTime).getSeconds() + 1;
                    throw new DataConflictException(
                            "Bạn thao tác quá nhanh. Vui lòng chờ " + secondsLeft + " giây nữa.");
                });
    }

    private BidPlacementResult applyProxyBid(BidContext context, BigDecimal newMaxBid) {
        AuctionSession session = context.session();
        User previousHighestBidder = session.getHighestBidder();
        BigDecimal previousHighestMaxBid = zeroIfNull(session.getHighestMaxBid());
        boolean reserveMetBefore = isReserveMetByMaxBid(session, previousHighestMaxBid);

        if (isCurrentHighestBidder(session, context.bidder())) {
            return raiseCurrentHighestMaxBid(context, newMaxBid, previousHighestBidder, reserveMetBefore);
        }

        if (newMaxBid.compareTo(previousHighestMaxBid) > 0) {
            return acceptNewHighestBidder(context, newMaxBid, previousHighestBidder, previousHighestMaxBid, reserveMetBefore);
        }

        return placeChallengerBidAndAutoBid(context, newMaxBid, previousHighestBidder, previousHighestMaxBid, reserveMetBefore);
    }

    private BidPlacementResult raiseCurrentHighestMaxBid(
            BidContext context,
            BigDecimal newMaxBid,
            User previousHighestBidder,
            boolean reserveMetBefore) {

        AuctionSession session = context.session();
        BigDecimal currentHighestMaxBid = zeroIfNull(session.getHighestMaxBid());
        if (newMaxBid.compareTo(currentHighestMaxBid) <= 0) {
            throw new DataConflictException(
                    "Giá mới phải cao hơn giá max hiện tại của bạn (" + currentHighestMaxBid + ")");
        }

        session.setHighestMaxBid(newMaxBid);
        if (shouldLiftPriceToReserve(session, newMaxBid)) {
            session.setCurrentPrice(session.getReservePrice());
        }

        Bid bid = saveBid(session, context.bidder(), newMaxBid, effectiveCurrentPrice(session), context.now());
        return new BidPlacementResult(
                bid,
                List.of(bid),
                previousHighestBidder,
                false,
                reserveMetBefore,
                isReserveMetByMaxBid(session, newMaxBid)
        );
    }

    private BidPlacementResult acceptNewHighestBidder(
            BidContext context,
            BigDecimal newMaxBid,
            User previousHighestBidder,
            BigDecimal previousHighestMaxBid,
            boolean reserveMetBefore) {

        AuctionSession session = context.session();
        BigDecimal newCurrentPrice = calculateWinningPrice(session, previousHighestBidder, previousHighestMaxBid, newMaxBid);
        session.setHighestBidder(context.bidder());
        session.setHighestMaxBid(newMaxBid);
        session.setCurrentPrice(newCurrentPrice);

        Bid bid = saveBid(session, context.bidder(), newMaxBid, session.getCurrentPrice(), context.now());
        return new BidPlacementResult(
                bid,
                List.of(bid),
                previousHighestBidder,
                true,
                reserveMetBefore,
                isReserveMetByMaxBid(session, newMaxBid)
        );
    }

    private BidPlacementResult placeChallengerBidAndAutoBid(
            BidContext context,
            BigDecimal challengerMaxBid,
            User currentHighestBidder,
            BigDecimal currentHighestMaxBid,
            boolean reserveMetBefore) {

        AuctionSession session = context.session();
        Bid challengerBid = saveBid(session, context.bidder(), challengerMaxBid, challengerMaxBid, context.now());

        BigDecimal autoBidPrice = calculateAutoBidPrice(session, currentHighestMaxBid, challengerMaxBid);
        session.setCurrentPrice(autoBidPrice);

        Bid autoBid = saveBid(
                session,
                currentHighestBidder,
                autoBidPrice,
                session.getCurrentPrice(),
                context.now().plusNanos(AUTO_BID_TIME_OFFSET_NANOS)
        );

        return new BidPlacementResult(
                autoBid,
                List.of(challengerBid, autoBid),
                currentHighestBidder,
                false,
                reserveMetBefore,
                isReserveMetByMaxBid(session, currentHighestMaxBid)
        );
    }

    private BigDecimal calculateWinningPrice(
            AuctionSession session,
            User previousHighestBidder,
            BigDecimal previousHighestMaxBid,
            BigDecimal newMaxBid) {

        if (previousHighestBidder == null) {
            return calculateFirstBidPrice(session, newMaxBid);
        }

        BigDecimal priceToBeat = previousHighestMaxBid.add(calculateIncrement(previousHighestMaxBid));
        BigDecimal newCurrentPrice = min(priceToBeat, newMaxBid);
        return applyReserveFloor(session, newMaxBid, newCurrentPrice);
    }

    private BigDecimal calculateFirstBidPrice(AuctionSession session, BigDecimal newMaxBid) {
        BigDecimal startPrice = session.getStartPrice();
        if (shouldLiftPriceToReserve(session, newMaxBid)) {
            return max(startPrice, session.getReservePrice());
        }
        return startPrice;
    }

    private BigDecimal calculateAutoBidPrice(
            AuctionSession session,
            BigDecimal currentHighestMaxBid,
            BigDecimal challengerMaxBid) {

        BigDecimal autoBidPrice = challengerMaxBid.add(calculateIncrement(challengerMaxBid));
        autoBidPrice = min(autoBidPrice, currentHighestMaxBid);
        return applyReserveFloor(session, currentHighestMaxBid, autoBidPrice);
    }

    private BigDecimal applyReserveFloor(AuctionSession session, BigDecimal maxBid, BigDecimal calculatedPrice) {
        if (shouldLiftPriceToReserve(session, maxBid)) {
            return max(calculatedPrice, session.getReservePrice());
        }
        return calculatedPrice;
    }

    private boolean shouldLiftPriceToReserve(AuctionSession session, BigDecimal maxBid) {
        return hasReservePrice(session)
                && maxBid.compareTo(session.getReservePrice()) >= 0
                && effectiveCurrentPrice(session).compareTo(session.getReservePrice()) < 0;
    }

    private Bid saveBid(
            AuctionSession session,
            User bidder,
            BigDecimal amount,
            BigDecimal resultingPrice,
            LocalDateTime bidTime) {

        return bidRepository.save(Bid.builder()
                .amount(amount)
                .bidTime(bidTime)
                .user(bidder)
                .auctionSession(session)
                .resultingPrice(resultingPrice)
                .build());
    }

    private void disableBuyNowAfterFirstBid(AuctionSession session, User previousHighestBidder) {
        if (session.getBuyNowPrice() != null && previousHighestBidder == null) {
            session.setBuyNowPrice(null);
        }
    }

    private BidResponse publishBidUpdates(Long auctionSessionId, AuctionSession session, BidPlacementResult result) {
        String roomName = "session-" + auctionSessionId;

        result.publishedBids().stream()
                .map(this::toBidResponse)
                .forEach(bidResponse -> socketIOService.sendMessageToRoom(
                        roomName,
                        SocketIOService.EVENT_NEW_BID,
                        bidResponse
                ));

        SimpleUserResponse highestBidderResponse = userMapper.userToSimpleUserResponse(session.getHighestBidder());
        Map<String, Object> priceUpdateData = Map.of(
                "currentPrice", effectiveCurrentPrice(session),
                "highestBidder", highestBidderResponse,
                "reservePriceMet", isReserveMetByCurrentPrice(session)
        );
        socketIOService.sendMessageToRoom(roomName, SocketIOService.EVENT_PRICE_UPDATE, priceUpdateData);

        return toBidResponse(result.displayBid());
    }

    private BidResponse toBidResponse(Bid bid) {
        BidResponse response = bidMapper.toBidResponse(bid);
        response.setDisplayedAmount(bid.getResultingPrice());
        return response;
    }

    private void sendBidNotifications(
            Bid displayBid,
            User bidder,
            User previousHighestBidder,
            boolean isNewHighestBidder,
            boolean reserveMetAfter,
            boolean reserveMetBefore,
            Product product) {

        String link = "/auction/" + displayBid.getAuctionSession().getId();
        String productName = product.getName();

        if (isNewHighestBidder) {
            notifyOutbidUser(previousHighestBidder, productName, displayBid.getResultingPrice(), link);
            notifyNewHighestBidder(bidder, productName, displayBid.getResultingPrice(), link, reserveMetAfter, reserveMetBefore);
            return;
        }

        if (previousHighestBidder != null && previousHighestBidder.getId().equals(bidder.getId())) {
            notifyCurrentLeaderIfReserveJustMet(bidder, productName, displayBid.getResultingPrice(), link, reserveMetAfter, reserveMetBefore);
            return;
        }

        notifyChallengerOutbid(bidder, previousHighestBidder, productName, displayBid.getResultingPrice(), link);
    }

    private void notifyOutbidUser(User previousHighestBidder, String productName, BigDecimal currentPrice, String link) {
        if (previousHighestBidder == null) {
            return;
        }

        String outbidMsg = String.format(
                "Bạn đã bị vượt qua trong phiên đấu giá '%s'. Giá hiện tại: '%s'",
                productName,
                currentPrice
        );
        notificationService.createNotification(previousHighestBidder, outbidMsg, link);
    }

    private void notifyNewHighestBidder(
            User bidder,
            String productName,
            BigDecimal currentPrice,
            String link,
            boolean reserveMetAfter,
            boolean reserveMetBefore) {

        String winnerMsg;
        if (reserveMetAfter) {
            if (!reserveMetBefore) {
                winnerMsg = String.format(
                        "Chúc mừng! Bạn là người đầu tiên đạt giá sàn cho '%s' và đang dẫn đầu với mức giá: '%s'.",
                        productName,
                        currentPrice
                );
            } else {
                winnerMsg = String.format(
                        "Bạn đang dẫn đầu phiên đấu giá '%s', mức giá hiện tại '%s'.",
                        productName,
                        currentPrice
                );
            }
        } else {
            winnerMsg = String.format(
                    "Bạn đang dẫn đầu phiên đấu giá '%s', nhưng giá sàn chưa được đáp ứng.",
                    productName
            );
        }
        notificationService.createNotification(bidder, winnerMsg, link);
    }

    private void notifyCurrentLeaderIfReserveJustMet(
            User bidder,
            String productName,
            BigDecimal currentPrice,
            String link,
            boolean reserveMetAfter,
            boolean reserveMetBefore) {

        if (reserveMetBefore || !reserveMetAfter) {
            return;
        }

        String message = String.format(
                "Giá tối đa mới của bạn đã đạt giá sàn cho '%s'. Giá hiện tại: '%s'.",
                productName,
                currentPrice
        );
        notificationService.createNotification(bidder, message, link);
    }

    private void notifyChallengerOutbid(
            User bidder,
            User previousHighestBidder,
            String productName,
            BigDecimal currentPrice,
            String link) {

        if (previousHighestBidder == null || previousHighestBidder.getId().equals(bidder.getId())) {
            return;
        }

        String notEnoughMsg = String.format(
                "Giá bạn đặt cho '%s' chưa đủ cao. Bạn đã bị vượt qua, mức giá hiện tại: '%s'",
                productName,
                currentPrice
        );
        notificationService.createNotification(bidder, notEnoughMsg, link);
    }

    private BigDecimal calculateIncrement(BigDecimal currentPrice) {
        BigDecimal price = currentPrice == null ? ZERO : currentPrice;
        if (price.compareTo(ZERO) <= 0) {
            return BID_INCREMENT_TIERS.getFirst().increment();
        }

        return BID_INCREMENT_TIERS.stream()
                .filter(tier -> price.compareTo(tier.exclusiveUpperBound()) < 0)
                .map(BidIncrementTier::increment)
                .findFirst()
                .orElse(DEFAULT_INCREMENT);
    }

    private BigDecimal calculateMinimumNextBid(BigDecimal currentPrice) {
        BigDecimal price = currentPrice == null ? ZERO : currentPrice;
        return price.add(calculateIncrement(price));
    }

    private boolean hasHighestBidder(AuctionSession session) {
        return session.getHighestBidder() != null;
    }

    private boolean isCurrentHighestBidder(AuctionSession session, User bidder) {
        return session.getHighestBidder() != null
                && session.getHighestBidder().getId().equals(bidder.getId());
    }

    private boolean hasReservePrice(AuctionSession session) {
        return session.getReservePrice() != null && session.getReservePrice().compareTo(ZERO) > 0;
    }

    private boolean isReserveMetByMaxBid(AuctionSession session, BigDecimal maxBid) {
        return !hasReservePrice(session) || zeroIfNull(maxBid).compareTo(session.getReservePrice()) >= 0;
    }

    private boolean isReserveMetByCurrentPrice(AuctionSession session) {
        return !hasReservePrice(session) || effectiveCurrentPrice(session).compareTo(session.getReservePrice()) >= 0;
    }

    private BigDecimal effectiveCurrentPrice(AuctionSession session) {
        return session.getCurrentPrice() != null ? session.getCurrentPrice() : session.getStartPrice();
    }

    private BigDecimal zeroIfNull(BigDecimal value) {
        return value == null ? ZERO : value;
    }

    private BigDecimal min(BigDecimal first, BigDecimal second) {
        return first.compareTo(second) <= 0 ? first : second;
    }

    private BigDecimal max(BigDecimal first, BigDecimal second) {
        return first.compareTo(second) >= 0 ? first : second;
    }

    private record BidIncrementTier(BigDecimal exclusiveUpperBound, BigDecimal increment) {
    }

    private record BidContext(AuctionSession session, Product product, User bidder, LocalDateTime now) {
    }

    private record BidPlacementResult(
            Bid displayBid,
            List<Bid> publishedBids,
            User previousHighestBidder,
            boolean newHighestBidder,
            boolean reserveMetBefore,
            boolean reserveMetAfter) {
    }
}
