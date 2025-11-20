package com.thanh.auction_server.repository;

import com.thanh.auction_server.constants.InvoiceStatus;
import com.thanh.auction_server.entity.Invoice;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface InvoiceRepository extends JpaRepository<Invoice, Long> {

    // Tìm các hóa đơn quá hạn để xử lý "bùng hàng"
    List<Invoice> findByStatusAndDueDateBefore(InvoiceStatus status, LocalDateTime now);

    // Lấy hóa đơn của tôi
     Page<Invoice> findByUser_IdOrderByCreatedAtDesc(String userId, Pageable pageable);
}
