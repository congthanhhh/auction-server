package com.thanh.auction_server.repository;

import com.thanh.auction_server.constants.InvoiceStatus;
import com.thanh.auction_server.constants.InvoiceType;
import com.thanh.auction_server.entity.Invoice;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface InvoiceRepository extends JpaRepository<Invoice, Long> {

    // Tìm các hóa đơn quá hạn để xử lý "bùng hàng"
    List<Invoice> findByStatusAndDueDateBefore(InvoiceStatus status, LocalDateTime now);

    // Lấy hóa đơn của tôi
    Page<Invoice> findByUser_IdOrderByCreatedAtDesc(String userId, Pageable pageable);

    List<Invoice> findByStatusAndShippedAtBefore(InvoiceStatus status, LocalDateTime date);

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
}
