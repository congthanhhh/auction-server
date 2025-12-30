package com.thanh.auction_server.repository;

import com.thanh.auction_server.constants.AuctionStatus;
import com.thanh.auction_server.entity.AuctionSession;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

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

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT a FROM AuctionSession a WHERE a.id = :id")
    Optional<AuctionSession> findByIdWithLock(@Param("id") Long id);

    // Tìm các phiên đấu giá theo trạng thái và sắp xếp theo createdAt (có phân trang)
    Page<AuctionSession> findByStatusOrderByCreatedAtAsc(AuctionStatus status, Pageable pageable);

    // Hoặc sắp xếp giảm dần (mới nhất trước) (có phân trang)
    Page<AuctionSession> findByStatusOrderByCreatedAtDesc(AuctionStatus status, Pageable pageable);



}
