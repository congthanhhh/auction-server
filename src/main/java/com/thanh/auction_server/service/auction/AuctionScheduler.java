package com.thanh.auction_server.service.auction;

import com.thanh.auction_server.entity.AuctionSession;
import com.thanh.auction_server.mapper.UserMapper;
import com.thanh.auction_server.service.utils.SocketIOService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class AuctionScheduler {
    private final AuctionSessionService auctionSessionService;
    private final SocketIOService socketIOService;
    private final UserMapper userMapper;

    @Scheduled(fixedRate = 30000) // 30 giây
    public void startScheduledAuctionsJob() {
        log.debug("Scheduler: Running job to start scheduled auctions...");
        try {
            // Nhận danh sách các phiên vừa BẮT ĐẦU
            List<AuctionSession> startedSessions = auctionSessionService.startScheduledAuctions();

            // Gửi thông báo WebSocket cho từng phiên
            for (AuctionSession session : startedSessions) {
                String roomName = "session-" + session.getId();
                socketIOService.sendMessageToRoom(roomName,
                        SocketIOService.EVENT_STATUS_CHANGE,
                        Map.of("status", session.getStatus().name()) // Gửi trạng thái "ACTIVE"
                );
                log.info("Scheduler: Đã bắt đầu phiên {} và gửi WebSocket.", session.getId());
            }
        } catch (Exception e) {
            log.error("Scheduler: Error during startScheduledAuctionsJob", e);
        }
    }

    @Scheduled(fixedRate = 30000) // 30 giây
    public void endActiveAuctionsJob() {
        log.debug("Scheduler: Running job to end active auctions...");
        try {
            // Nhận danh sách các phiên vừa KẾT THÚC
            List<AuctionSession> endedSessions = auctionSessionService.endActiveAuctions();

            // Gửi thông báo WebSocket cho từng phiên
            for (AuctionSession session : endedSessions) {
                String roomName = "session-" + session.getId();

                // Chuẩn bị dữ liệu kết quả cuối cùng
                var finalResult = Map.of(
                        "status", session.getStatus().name(),
                        "finalPrice", session.getCurrentPrice(),
                        // Kiểm tra null cho highestBidder trước khi map
                        "winner", session.getHighestBidder() != null
                                ? userMapper.userToSimpleUserResponse(session.getHighestBidder())
                                : null
                );

                socketIOService.sendMessageToRoom(roomName, SocketIOService.EVENT_AUCTION_ENDED, finalResult);
                log.info("Scheduler: Đã kết thúc phiên {} và gửi WebSocket.", session.getId());
            }
        } catch (Exception e) {
            log.error("Scheduler: Error during endActiveAuctionsJob", e);
        }
    }
}
