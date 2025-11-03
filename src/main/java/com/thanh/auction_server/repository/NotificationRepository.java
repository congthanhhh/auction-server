package com.thanh.auction_server.repository;

import com.thanh.auction_server.entity.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    // Lấy thông báo cho 1 user, sắp xếp mới nhất trước
    Page<Notification> findByUser_IdOrderByCreatedAtDesc(String userId, Pageable pageable);

    // Đếm số thông báo chưa đọc
    long countByUser_IdAndIsReadFalse(String userId);

    // Đánh dấu tất cả là đã đọc (Ví dụ)
    @Modifying
    @Query("UPDATE Notification n SET n.isRead = true WHERE n.user.id = :userId AND n.isRead = false")
    void markAllAsReadByUserId(String userId);
}
