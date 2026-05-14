package com.thanh.auction_server.service.auction;

import com.thanh.auction_server.constants.AuctionStatus;
import com.thanh.auction_server.dto.request.BidRequest;
import com.thanh.auction_server.dto.response.BidResponse;
import com.thanh.auction_server.dto.response.SimpleUserResponse;
import com.thanh.auction_server.entity.AuctionSession;
import com.thanh.auction_server.entity.Bid;
import com.thanh.auction_server.entity.Product;
import com.thanh.auction_server.entity.User;
import com.thanh.auction_server.mapper.BidMapper;
import com.thanh.auction_server.mapper.UserMapper;
import com.thanh.auction_server.repository.AuctionSessionRepository;
import com.thanh.auction_server.repository.BidRepository;
import com.thanh.auction_server.repository.UserRepository;
import com.thanh.auction_server.service.utils.SocketIOService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BidServiceTests {
    @Mock
    BidRepository bidRepository;
    @Mock
    BidMapper bidMapper;
    @Mock
    AuctionSessionRepository auctionSessionRepository;
    @Mock
    UserRepository userRepository;
    @Mock
    UserMapper userMapper;
    @Mock
    NotificationService notificationService;
    @Mock
    SocketIOService socketIOService;

    BidService bidService;
    AtomicLong bidIds;

    @BeforeEach
    void setUp() {
        bidService = new BidService(
                bidRepository,
                bidMapper,
                auctionSessionRepository,
                userRepository,
                userMapper,
                notificationService,
                socketIOService
        );
        bidIds = new AtomicLong(1);

        when(bidRepository.save(any(Bid.class))).thenAnswer(invocation -> {
            Bid bid = invocation.getArgument(0);
            bid.setId(bidIds.getAndIncrement());
            return bid;
        });
        when(bidRepository.findLastBidTimeBySessionAndUser(anyLong(), anyString())).thenReturn(Optional.empty());
        when(bidMapper.toBidResponse(any(Bid.class))).thenAnswer(invocation -> toBidResponse(invocation.getArgument(0)));
        when(userMapper.userToSimpleUserResponse(any(User.class))).thenAnswer(invocation -> toSimpleUser(invocation.getArgument(0)));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void firstBidWithoutReserveIsReserveMetAndDisablesBuyNow() {
        User seller = user("seller");
        User bidder = user("bidder");
        AuctionSession session = activeSession(seller);
        session.setBuyNowPrice(bd("250000"));
        authenticateAs("bidder");
        when(auctionSessionRepository.findByIdWithLock(1L)).thenReturn(Optional.of(session));
        when(userRepository.findByUsername("bidder")).thenReturn(Optional.of(bidder));

        BidResponse response = bidService.placeBid(1L, BidRequest.builder().amount(bd("150000")).build());

        assertThat(response.getDisplayedAmount()).isEqualByComparingTo("100000");
        assertThat(session.getHighestBidder()).isSameAs(bidder);
        assertThat(session.getHighestMaxBid()).isEqualByComparingTo("150000");
        assertThat(session.getCurrentPrice()).isEqualByComparingTo("100000");
        assertThat(session.getBuyNowPrice()).isNull();

        ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);
        verify(notificationService).createNotification(eq(bidder), messageCaptor.capture(), eq("/auction/1"));
        assertThat(messageCaptor.getValue()).doesNotContain("giá sàn chưa");

        ArgumentCaptor<Object> payloadCaptor = ArgumentCaptor.forClass(Object.class);
        verify(socketIOService).sendMessageToRoom(
                eq("session-1"),
                eq(SocketIOService.EVENT_PRICE_UPDATE),
                payloadCaptor.capture()
        );
        Map<?, ?> payload = (Map<?, ?>) payloadCaptor.getValue();
        assertThat(payload.get("currentPrice")).isEqualTo(bd("100000"));
        assertThat(payload.get("reservePriceMet")).isEqualTo(true);
    }

    @Test
    void challengerBelowExistingMaxCreatesAutoBidAndKeepsCurrentLeader() {
        User seller = user("seller");
        User leader = user("leader");
        User challenger = user("challenger");
        AuctionSession session = activeSession(seller);
        session.setHighestBidder(leader);
        session.setHighestMaxBid(bd("200000"));
        session.setCurrentPrice(bd("100000"));
        authenticateAs("challenger");
        when(auctionSessionRepository.findByIdWithLock(1L)).thenReturn(Optional.of(session));
        when(userRepository.findByUsername("challenger")).thenReturn(Optional.of(challenger));

        BidResponse response = bidService.placeBid(1L, BidRequest.builder().amount(bd("120000")).build());

        assertThat(response.getDisplayedAmount()).isEqualByComparingTo("130000");
        assertThat(response.getUser().getId()).isEqualTo(leader.getId());
        assertThat(session.getHighestBidder()).isSameAs(leader);
        assertThat(session.getHighestMaxBid()).isEqualByComparingTo("200000");
        assertThat(session.getCurrentPrice()).isEqualByComparingTo("130000");

        ArgumentCaptor<Bid> bidCaptor = ArgumentCaptor.forClass(Bid.class);
        verify(bidRepository, times(2)).save(bidCaptor.capture());
        List<Bid> savedBids = bidCaptor.getAllValues();
        assertThat(savedBids.get(0).getUser()).isSameAs(challenger);
        assertThat(savedBids.get(0).getResultingPrice()).isEqualByComparingTo("120000");
        assertThat(savedBids.get(1).getUser()).isSameAs(leader);
        assertThat(savedBids.get(1).getResultingPrice()).isEqualByComparingTo("130000");

        verify(socketIOService, times(2)).sendMessageToRoom(
                eq("session-1"),
                eq(SocketIOService.EVENT_NEW_BID),
                any(BidResponse.class)
        );
        verify(notificationService).createNotification(eq(challenger), anyString(), eq("/auction/1"));
    }

    @Test
    void currentLeaderRaisingMaxToReserveMovesCurrentPriceToReserve() {
        User seller = user("seller");
        User bidder = user("bidder");
        AuctionSession session = activeSession(seller);
        session.setReservePrice(bd("200000"));
        session.setHighestBidder(bidder);
        session.setHighestMaxBid(bd("150000"));
        session.setCurrentPrice(bd("100000"));
        authenticateAs("bidder");
        when(auctionSessionRepository.findByIdWithLock(1L)).thenReturn(Optional.of(session));
        when(userRepository.findByUsername("bidder")).thenReturn(Optional.of(bidder));

        BidResponse response = bidService.placeBid(1L, BidRequest.builder().amount(bd("250000")).build());

        assertThat(response.getDisplayedAmount()).isEqualByComparingTo("200000");
        assertThat(session.getHighestBidder()).isSameAs(bidder);
        assertThat(session.getHighestMaxBid()).isEqualByComparingTo("250000");
        assertThat(session.getCurrentPrice()).isEqualByComparingTo("200000");

        ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);
        verify(notificationService).createNotification(eq(bidder), messageCaptor.capture(), eq("/auction/1"));
        assertThat(messageCaptor.getValue()).contains("đã đạt giá sàn");
    }

    private AuctionSession activeSession(User seller) {
        Product product = Product.builder()
                .id(10L)
                .name("Test product")
                .startPrice(bd("100000"))
                .seller(seller)
                .build();

        return AuctionSession.builder()
                .id(1L)
                .product(product)
                .startPrice(product.getStartPrice())
                .currentPrice(product.getStartPrice())
                .status(AuctionStatus.ACTIVE)
                .startTime(LocalDateTime.now().minusMinutes(1))
                .endTime(LocalDateTime.now().plusMinutes(10))
                .build();
    }

    private User user(String username) {
        return User.builder()
                .id(username + "-id")
                .username(username)
                .email(username + "@example.com")
                .strikeCount(0)
                .build();
    }

    private void authenticateAs(String username) {
        var securityContext = SecurityContextHolder.createEmptyContext();
        securityContext.setAuthentication(new UsernamePasswordAuthenticationToken(username, "password"));
        SecurityContextHolder.setContext(securityContext);
    }

    private BidResponse toBidResponse(Bid bid) {
        return BidResponse.builder()
                .id(bid.getId())
                .displayedAmount(bid.getResultingPrice())
                .bidTime(bid.getBidTime())
                .user(toSimpleUser(bid.getUser()))
                .auctionSessionId(bid.getAuctionSession().getId())
                .build();
    }

    private SimpleUserResponse toSimpleUser(User user) {
        return SimpleUserResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .build();
    }

    private BigDecimal bd(String value) {
        return new BigDecimal(value);
    }
}
