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

    // Tìm bid cao nhất (Max Bid) của một người dùng cụ thể cho một phiên
    // Optional<Bid> findTopByUser_IdAndAuctionSession_IdOrderByAmountDesc(String userId, Long auctionSessionId);

    // Tìm bid cao nhất (Max Bid) hiện tại của phiên đấu giá (không phân biệt user)
    // Dùng JPQL để lấy bid có amount cao nhất, nếu bằng nhau thì lấy cái cũ hơn (bidTime nhỏ hơn)
    @Query("SELECT b FROM Bid b WHERE b.auctionSession.id = :sessionId ORDER BY b.amount DESC, b.bidTime ASC")
    List<Bid> findHighestBidsForSession(Long sessionId, Pageable pageable); // Pageable(0, 1) để lấy top 1

    // Đếm số lượng bid cho một phiên
    // long countByAuctionSessionId(Long auctionSessionId);
}
