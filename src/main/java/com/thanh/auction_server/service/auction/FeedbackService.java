package com.thanh.auction_server.service.auction;

import com.thanh.auction_server.constants.ErrorMessage;
import com.thanh.auction_server.constants.InvoiceStatus;
import com.thanh.auction_server.dto.request.FeedBackRequest;
import com.thanh.auction_server.dto.response.MessageResponse;
import com.thanh.auction_server.entity.FeedBack;
import com.thanh.auction_server.entity.Invoice;
import com.thanh.auction_server.entity.User;
import com.thanh.auction_server.exception.DataConflictException;
import com.thanh.auction_server.exception.ResourceNotFoundException;
import com.thanh.auction_server.exception.UnauthorizedException;
import com.thanh.auction_server.repository.FeedbackRepository;
import com.thanh.auction_server.repository.InvoiceRepository;
import com.thanh.auction_server.repository.UserRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
@Service
public class FeedbackService {
    FeedbackRepository feedbackRepository;
    InvoiceRepository invoiceRepository;
    UserRepository userRepository;

    @Transactional
    public MessageResponse createFeedback(Long invoiceId, FeedbackRequest request) {
        String currentUsername = SecurityContextHolder.getContext().getAuthentication().getName();
        User currentUser = userRepository.findByUsername(currentUsername)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice not found with id: " + invoiceId));

        boolean isBuyerEvaluating = currentUser.getId().equals(invoice.getUser().getId());
        boolean isSellerEvaluating = currentUser.getId().equals(invoice.getProduct().getSeller().getId());

        if (!isBuyerEvaluating && !isSellerEvaluating) {
            throw new UnauthorizedException("Bạn không tham gia vào giao dịch này nên không có quyền đánh giá.");
        }

        // 2. CHECK QUYỀN ĐÁNH GIÁ DỰA TRÊN TRẠNG THÁI HÓA ĐƠN
        if (invoice.getStatus() == InvoiceStatus.PAID) {
            // Cho phép cả 2 bên đánh giá
        }
        else if (invoice.getStatus() == InvoiceStatus.CANCELLED_NON_PAYMENT) {
            if (isBuyerEvaluating) {
                throw new DataConflictException("Bạn không thể đánh giá vì bạn đã không thanh toán hóa đơn này (Bùng hàng).");
            }
        }
        // Các trường hợp khác (PENDING, CANCELLED_BY_SELLER...)
        else {
            throw new DataConflictException("Giao dịch chưa hoàn tất hoặc đã bị hủy, chưa thể đánh giá.");
        }

        // 3. Kiểm tra xem đã đánh giá chưa (Tránh spam)
        if (feedbackRepository.existsByInvoice_IdAndFromUser_Id(invoiceId, currentUser.getId())) {
            throw new DataConflictException("Bạn đã gửi đánh giá cho giao dịch này rồi.");
        }

        // 4. Xác định người nhận (Target User)
        User toUser = isBuyerEvaluating ? invoice.getProduct().getSeller() : invoice.getUser();

        // 5. Lưu đánh giá
        FeedBack feedback = FeedBack.builder()
                .fromUser(currentUser)
                .toUser(toUser)
                .invoice(invoice)
                .rating(request.getRating())
                .comment(request.getComment())
                .createdAt(LocalDateTime.now())
                .build();

        feedbackRepository.save(feedback);

        return MessageResponse.builder()
                .message("Gửi đánh giá thành công!")
                .build();
    }
}
