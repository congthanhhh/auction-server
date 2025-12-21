package com.thanh.auction_server.service.auction;

import com.thanh.auction_server.constants.ErrorMessage;
import com.thanh.auction_server.constants.FeedbackRating;
import com.thanh.auction_server.constants.InvoiceStatus;
import com.thanh.auction_server.dto.request.DisputeRequest;
import com.thanh.auction_server.dto.request.ShipInvoiceRequest;
import com.thanh.auction_server.dto.response.InvoiceResponse;
import com.thanh.auction_server.dto.response.MessageResponse;
import com.thanh.auction_server.dto.response.PageResponse;
import com.thanh.auction_server.entity.*;
import com.thanh.auction_server.exception.DataConflictException;
import com.thanh.auction_server.exception.ResourceNotFoundException;
import com.thanh.auction_server.exception.UnauthorizedException;
import com.thanh.auction_server.mapper.InvoiceMapper;
import com.thanh.auction_server.repository.DisputeRepository;
import com.thanh.auction_server.repository.FeedbackRepository;
import com.thanh.auction_server.repository.InvoiceRepository;
import com.thanh.auction_server.repository.UserRepository;
import com.thanh.auction_server.service.authenticate.UserService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
@Service
public class InvoiceService {
    InvoiceRepository invoiceRepository;
    DisputeRepository disputeRepository;
    UserRepository userRepository;
    FeedbackRepository feedbackRepository;
    InvoiceMapper invoiceMapper;
    UserService userService;
    NotificationService notificationService;

    @Transactional
    public void createInvoiceForWinner(AuctionSession session, User winner) {
        LocalDateTime dueDate = LocalDateTime.now().plusDays(4);

        Invoice invoice = Invoice.builder()
                .user(winner)
                .auctionSession(session)
                .product(session.getProduct())
                .finalPrice(session.getCurrentPrice())
                .status(InvoiceStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .dueDate(dueDate)
                .build();

        invoiceRepository.save(invoice);
        log.info("Invoice created (ID: {}) cho người thắng {} của phiên {}", invoice.getId(), winner.getUsername(), session.getId());

    }

    public PageResponse<InvoiceResponse> getMyInvoices(int page, int size) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorMessage.USER_NOT_FOUND));
        Pageable pageable = PageRequest.of(page - 1, size);

        Page<Invoice> invoicePage = invoiceRepository.findByUser_IdOrderByCreatedAtDesc(user.getId(), pageable);
        List<InvoiceResponse> responses = invoicePage.getContent()
                .stream()
                .map(invoiceMapper::toInvoiceResponse)
                .collect(Collectors.toList());

        return PageResponse.<InvoiceResponse>builder()
                .currentPage(page)
                .totalPages(invoicePage.getTotalPages())
                .pageSize(invoicePage.getSize())
                .totalElements(invoicePage.getTotalElements())
                .data(responses)
                .build();
    }

    public InvoiceResponse getInvoiceById(Long id) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        Invoice invoice = invoiceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorMessage.INVOICE_NOT_FOUND + id));

        if (!invoice.getUser().getUsername().equals(username)) {
            log.warn("User '{}' cố gắng truy cập hóa đơn ID {} của user '{}'",
                    username, id, invoice.getUser().getUsername());
            throw new UnauthorizedException(ErrorMessage.UNAUTHORIZED);
        }

        return invoiceMapper.toInvoiceResponse(invoice);
    }

    @Transactional
    public MessageResponse reportNonPayment(Long invoiceId) {
        String currentUsername = SecurityContextHolder.getContext().getAuthentication().getName();

        // 1. Tìm hóa đơn
        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy hóa đơn với ID: " + invoiceId));

        User seller = invoice.getProduct().getSeller();
        User buyer = invoice.getUser();

        // 2. Kiểm tra bảo mật: Chỉ người bán mới có quyền báo cáo
        if (!seller.getUsername().equals(currentUsername)) {
            log.warn("User '{}' cố gắng báo cáo hóa đơn ID {} không thuộc sở hữu của họ.", currentUsername, invoiceId);
            throw new UnauthorizedException("Bạn không có quyền thực hiện hành động này.");
        }

        // 3. Kiểm tra logic nghiệp vụ
        if (invoice.getStatus() != InvoiceStatus.PENDING) {
            throw new DataConflictException("Chỉ có thể báo cáo hóa đơn đang chờ thanh toán.");
        }

        // 4. Kiểm tra hạn chót thanh toán (dueDate)
        if (invoice.getDueDate().isAfter(LocalDateTime.now())) {
            throw new DataConflictException("Bạn chỉ có thể báo cáo sau khi đã quá hạn thanh toán.");
        }

        // 5. THỰC THI HÀNH ĐỘNG
        invoice.setStatus(InvoiceStatus.CANCELLED_NON_PAYMENT);
        invoiceRepository.save(invoice);
        userService.incrementStrikeCount(buyer.getId());
        Feedback autoNegativeFeedback = Feedback.builder()
                .fromUser(seller)
                .toUser(buyer)
                .invoice(invoice)
                .rating(FeedbackRating.NEGATIVE)
                .comment("HỆ THỐNG TỰ ĐỘNG: Người mua không thanh toán hóa đơn này.")
                .createdAt(LocalDateTime.now())
                .build();
        feedbackRepository.save(autoNegativeFeedback);
        // update điẻm uy tín
        int currentScore = buyer.getReputationScore() == null ? 0 : buyer.getReputationScore();
        buyer.setReputationScore(currentScore - 1);
        userRepository.save(buyer);

        notificationService.createNotification(buyer, "Bạn bị trừ 1 điểm uy tín và nhận 1 gậy phạt do bùng hàng.", "/profile");

        return MessageResponse.builder()
                .message("Đã báo cáo bùng hàng. Người mua đã bị phạt điểm uy tín và nhận 1 gậy.")
                .build();
    }

    @Transactional
    public MessageResponse shipInvoice(Long invoiceId, ShipInvoiceRequest request) {
        String currentUsername = SecurityContextHolder.getContext().getAuthentication().getName();
        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorMessage.INVOICE_NOT_FOUND + invoiceId));
        User seller = invoice.getProduct().getSeller();
        if (!seller.getUsername().equals(currentUsername)) {
            throw new UnauthorizedException(ErrorMessage.UNAUTHORIZED);
        }
        if (invoice.getStatus() != InvoiceStatus.PAID) {
            throw new DataConflictException(ErrorMessage.STATUS_INCORRECT + invoice.getStatus());
        }
        invoice.setTrackingCode(request.getTrackingCode());
        invoice.setCarrier(request.getCarrier());
        invoice.setShippedAt(LocalDateTime.now());
        invoice.setStatus(InvoiceStatus.SHIPPING);
        invoiceRepository.save(invoice);

        User buyer = invoice.getUser();
        String message = "Đơn hàng '" + invoice.getProduct().getName() + "' đã được gửi đi. Mã vận đơn: " + request.getTrackingCode();
        String link = "/invoices/" + invoice.getId(); // Link đến chi tiết đơn hàng
        notificationService.createNotification(buyer, message, link);

        return MessageResponse.builder()
                .message("Cập nhật vận đơn thành công. Đơn hàng đang trên đường giao.")
                .build();
    }

    @Transactional
    public MessageResponse confirmInvoice(Long invoiceId) {
        String currentUsername = SecurityContextHolder.getContext().getAuthentication().getName();
        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorMessage.INVOICE_NOT_FOUND + invoiceId));
        User buyer = invoice.getUser();
        if (!buyer.getUsername().equals(currentUsername)) {
            throw new UnauthorizedException(ErrorMessage.UNAUTHORIZED);
        }
        if (invoice.getStatus() != InvoiceStatus.SHIPPING) {
            throw new DataConflictException(ErrorMessage.STATUS_INCORRECT + invoice.getStatus());
        }
        invoice.setStatus(InvoiceStatus.COMPLETED);
        invoiceRepository.save(invoice);

        User seller = invoice.getProduct().getSeller();
        String sellerMsg = "Người mua " + buyer.getUsername() + " đã xác nhận nhận đơn hàng '" + invoice.getProduct().getName() + "'. Giao dịch hoàn tất.";
        notificationService.createNotification(seller, sellerMsg, "/invoices/" + invoice.getId());

        String buyerMsg = "Cảm ơn bạn đã mua hàng! Hãy dành chút thời gian đánh giá người bán.";
        notificationService.createNotification(buyer, buyerMsg, "/invoices/" + invoice.getId());

        return MessageResponse.builder()
                .message("Xác nhận nhận hàng thành công. Giao dịch đã hoàn tất.")
                .build();
    }

    @Transactional
    public MessageResponse reportDispute(Long invoiceId, DisputeRequest request) {
        String currentUsername = SecurityContextHolder.getContext().getAuthentication().getName();
        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorMessage.INVOICE_NOT_FOUND + invoiceId));
        User buyer = invoice.getUser();
        if (!buyer.getUsername().equals(currentUsername)) {
            throw new UnauthorizedException(ErrorMessage.UNAUTHORIZED);
        }
        if (invoice.getStatus() != InvoiceStatus.SHIPPING) {
            throw new DataConflictException(ErrorMessage.STATUS_INCORRECT + invoice.getStatus());
        }
        invoice.setStatus(InvoiceStatus.DISPUTE);
        invoiceRepository.save(invoice);

        Dispute dispute = Dispute.builder()
                .invoice(invoice)
                .reason(request.getReason())
                .createdAt(LocalDateTime.now())
                .build();
        disputeRepository.save(dispute);

        User seller = invoice.getProduct().getSeller();
        // A. Báo cho Seller
        String sellerMsg = "Người mua ĐANG KHIẾU NẠI đơn hàng '" + invoice.getProduct().getName() + "'. Lý do: " + request.getReason();
        notificationService.createNotification(seller, sellerMsg, "/invoices/" + invoice.getId());
        // B. Báo cho Buyer xác nhận
        String buyerMsg = "Khiếu nại của bạn đã được ghi nhận. Hệ thống đã tạm dừng quy trình hoàn thành đơn hàng.";
        notificationService.createNotification(buyer, buyerMsg, "/invoices/" + invoice.getId());

        return MessageResponse.builder()
                .message("Đã gửi khiếu nại thành công. Admin và Người bán sẽ sớm liên hệ giải quyết.")
                .build();
    }

    @Transactional
    public void autoFinishInvoices() {
        //Tự động hoàn thành sau 15 ngày
        LocalDateTime thresholdTime = LocalDateTime.now().minusDays(15);
        // Tìm các đơn SHIPPING đã quá hạn
        List<Invoice> invoices = invoiceRepository.findByStatusAndShippedAtBefore(InvoiceStatus.SHIPPING, thresholdTime);
        if (invoices.isEmpty()) {
            return;
        }
        log.info("Tìm thấy {} đơn hàng SHIPPING quá 15 ngày. Đang tự động hoàn thành...", invoices.size());
        for (Invoice invoice : invoices) {
            invoice.setStatus(InvoiceStatus.COMPLETED);
//            notificationService.createNotification(
//                    invoice.getUser(),
//                    "Đơn hàng '" + invoice.getProduct().getName() + "' đã được hệ thống tự động xác nhận hoàn thành.",
//                    "/invoices/" + invoice.getId()
//            );
            notificationService.createNotification(
                    invoice.getProduct().getSeller(),
                    "Đơn hàng '" + invoice.getProduct().getName() + "' đã tự động hoàn tất sau 15 ngày.",
                    "/invoices/" + invoice.getId()
            );
        }
        invoiceRepository.saveAll(invoices);
    }

    @Transactional
    public void createFeeInvoiceForSeller(AuctionSession session, String feeType) {
        // ... Logic tính phí ...
        // BigDecimal fee = BigDecimal.valueOf(10000); // Ví dụ 10,000đ
        // Invoice invoice = Invoice.builder()
        //     .user(session.getProduct().getSeller()) // Người bán
        //     .auctionSession(session)
        //     .product(session.getProduct())
        //     .finalPrice(fee)
        //     .status(InvoiceStatus.PENDING)
        //     // ...
        // invoiceRepository.save(invoice);
        log.info("Tạm thời chưa implement thu phí giá sàn.");
    }
}
