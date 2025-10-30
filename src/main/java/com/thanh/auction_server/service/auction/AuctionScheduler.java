package com.thanh.auction_server.service.auction;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class AuctionScheduler {
    private final AuctionSessionService auctionSessionService;

    @Scheduled(fixedRate = 30000) //30 giây
    public void startScheduledAuctionsJob() {
        log.debug("Scheduler: Running job to start scheduled auctions...");
        try {
            auctionSessionService.startScheduledAuctions();
        } catch (Exception e) {
            log.error("Scheduler: Error during startScheduledAuctionsJob", e);
        }
    }
    @Scheduled(fixedRate = 30000) // 30,000 mili giây = 30 giây
    public void endActiveAuctionsJob() {
        log.debug("Scheduler: Running job to end active auctions...");
        try {
            auctionSessionService.endActiveAuctions();
        } catch (Exception e) {
            log.error("Scheduler: Error during endActiveAuctionsJob", e);
        }
    }
}
