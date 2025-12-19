package com.thanh.auction_server.repository;

import com.thanh.auction_server.constants.AuctionStatus;
import com.thanh.auction_server.entity.AuctionSession;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.time.LocalDateTime;
import java.util.List;

public interface AuctionSessionRepository extends JpaRepository<AuctionSession, Long>,
        JpaSpecificationExecutor<AuctionSession> {

    // Tìm các phiên đấu giá theo trạng thái (có phân trang)
    Page<AuctionSession> findByStatus(AuctionStatus status, Pageable pageable);

    // Tìm các phiên đấu giá sắp bắt đầu (status = SCHEDULED và startTime <= now)
    List<AuctionSession> findByStatusAndStartTimeLessThanEqual(AuctionStatus status, LocalDateTime now);

    // Tìm các phiên đấu giá đang hoạt động và sắp kết thúc (status = ACTIVE và endTime <= now)
    List<AuctionSession> findByStatusAndEndTimeLessThanEqual(AuctionStatus status, LocalDateTime now);

    // Tìm phiên đấu giá bằng ID sản phẩm (vì là OneToOne)
    // Optional<AuctionSession> findByProductId(Long productId);

    boolean existsByProduct_Id(Long productId);

}
