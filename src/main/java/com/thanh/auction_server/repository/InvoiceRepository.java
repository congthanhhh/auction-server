package com.thanh.auction_server.repository;

import com.thanh.auction_server.constants.InvoiceStatus;
import com.thanh.auction_server.constants.InvoiceType;
import com.thanh.auction_server.entity.Invoice;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public interface InvoiceRepository extends JpaRepository<Invoice, Long> {

    // Tìm các hóa đơn quá hạn để xử lý "bùng hàng"
    List<Invoice> findByStatusAndDueDateBefore(InvoiceStatus status, LocalDateTime now);

    // Lấy hóa đơn của tôi
    Page<Invoice> findByUser_IdOrderByCreatedAtDesc(String userId, Pageable pageable);

    List<Invoice> findByStatusAndShippedAtBefore(InvoiceStatus status, LocalDateTime date);

    boolean existsByAuctionSessionIdAndType(Long auctionSessionId, InvoiceType type);

    @Query("SELECT i FROM Invoice i " +
            "WHERE i.product.seller.username = :username " +
            "AND (:status IS NULL OR i.status = :status) " +
            "ORDER BY i.createdAt DESC")
    Page<Invoice> findBySellerUsernameAndStatus(
            @Param("username") String username,
            @Param("status") InvoiceStatus status,
            Pageable pageable);

    @Query("SELECT i FROM Invoice i " +
            "WHERE i.product.seller.username = :username " +
            "AND (:status IS NULL OR i.status = :status) " +
            "AND (:type IS NULL OR i.type = :type) " +
            "ORDER BY i.createdAt DESC")
    Page<Invoice> findBySellerUsernameStatusAndType(
            @Param("username") String username,
            @Param("status") InvoiceStatus status,
            @Param("type") InvoiceType type,
            Pageable pageable);

    @Query("SELECT COALESCE(SUM(i.finalPrice), 0) FROM Invoice i " +
            "WHERE i.product.seller.username = :username " +
            "AND i.type = 'AUCTION_SALE' " +
            "AND i.status IN :statuses")
    Long sumRevenueBySellerAndStatus(
            @Param("username") String username,
            @Param("statuses") List<InvoiceStatus> statuses);

    @Query("SELECT i FROM Invoice i " +
            "WHERE i.user.username = :username " +
            "AND (:status IS NULL OR i.status = :status) " +
            "AND (:type IS NULL OR i.type = :type) " +
            "ORDER BY i.createdAt DESC")
    Page<Invoice> findByUserUsernameAndStatusAndType(
            @Param("username") String username,
            @Param("status") InvoiceStatus status,
            @Param("type") InvoiceType type,
            Pageable pageable);

    // Gia lap doanh thu
    @Query("SELECT SUM(i.finalPrice) FROM Invoice i WHERE i.status = 'PAID' AND i.type = 'LISTING_FEE'")
    BigDecimal sumTotalListingFee();
    @Query("SELECT SUM(i.finalPrice) FROM Invoice i WHERE i.status = 'COMPLETED' AND i.type = 'AUCTION_SALE'")
    BigDecimal sumTotalAuctionSales();

    // 1. Tính tổng phí niêm yết trong khoảng thời gian (Listing Fee)
    @Query("SELECT SUM(i.finalPrice) FROM Invoice i " +
            "WHERE i.status = 'PAID' AND i.type = 'LISTING_FEE' " +
            "AND (:startDate IS NULL OR i.createdAt >= :startDate) " +
            "AND (:endDate IS NULL OR i.createdAt <= :endDate)")
    BigDecimal sumListingFeeBetween(@Param("startDate") LocalDateTime startDate,
                                    @Param("endDate") LocalDateTime endDate);

    // 2. Tính tổng giá trị giao dịch trong khoảng thời gian (Auction Sales)
    @Query("SELECT SUM(i.finalPrice) FROM Invoice i " +
            "WHERE i.status = 'COMPLETED' AND i.type = 'AUCTION_SALE' " +
            "AND (:startDate IS NULL OR i.createdAt >= :startDate) " +
            "AND (:endDate IS NULL OR i.createdAt <= :endDate)")
    BigDecimal sumAuctionSalesBetween(@Param("startDate") LocalDateTime startDate,
                                      @Param("endDate") LocalDateTime endDate);
}
