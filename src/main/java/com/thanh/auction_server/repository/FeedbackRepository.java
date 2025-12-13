package com.thanh.auction_server.repository;

import com.thanh.auction_server.entity.Feedback;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface FeedbackRepository extends JpaRepository<Feedback, Long> {
    // Tìm danh sách feedback mà user này NHẬN được (để hiển thị trên profile)
    Page<Feedback> findByToUser_IdOrderByCreatedAtDesc(String toUserId, Pageable pageable);

    // Kiểm tra xem (fromUser) đã đánh giá cho hóa đơn (invoice) này chưa
    boolean existsByInvoice_IdAndFromUser_Id(Long invoiceId, String fromUserId);

    // Tính điểm trung bình (Uy tín) của một user
    @Query("SELECT AVG(f.rating) FROM Feedback f WHERE f.toUser.id = :userId")
    Double getAverageRating(String userId);

    // Đếm số lượng đánh giá
    long countByToUser_Id(String userId);
}
