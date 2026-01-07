package com.thanh.auction_server.repository;

import com.thanh.auction_server.entity.Bid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface BidRepository extends JpaRepository<Bid, Long> {
    // Tìm tất cả các bid của một phiên đấu giá, sắp xếp theo thời gian (mới nhất trước)
    Page<Bid> findByAuctionSessionIdOrderByBidTimeDesc(Long auctionSessionId, Pageable pageable);

    // Tìm bid cao nhất (Max Bid) hiện tại của phiên đấu giá (không phân biệt user)
    @Query("SELECT b FROM Bid b WHERE b.auctionSession.id = :sessionId ORDER BY b.amount DESC, b.bidTime ASC")
    List<Bid> findHighestBidsForSession(Long sessionId, Pageable pageable); // Pageable(0, 1) để lấy top 1

    // Đếm số lượng bid cho một phiên
     long countByAuctionSessionId(Long auctionSessionId);

    long countByAuctionSession_Product_Id(Long productId);

}



//Use countByAuctionSessionId when you want to count bids for a single auction session
//Use countByAuctionSession_Product_Id when you want to count total bids across all auction sessions for a product (a product might have multiple auction sessions)