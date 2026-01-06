package com.thanh.auction_server.service.invoice;

import com.thanh.auction_server.constants.*;
import com.thanh.auction_server.dto.request.DisputeRequest;
import com.thanh.auction_server.dto.request.DisputeSearchRequest;
import com.thanh.auction_server.dto.request.ResolveDisputeRequest;
import com.thanh.auction_server.dto.request.ShipInvoiceRequest;
import com.thanh.auction_server.dto.response.DisputeResponse;
import com.thanh.auction_server.dto.response.InvoiceResponse;
import com.thanh.auction_server.dto.response.MessageResponse;
import com.thanh.auction_server.dto.response.PageResponse;
import com.thanh.auction_server.entity.*;
import com.thanh.auction_server.exception.DataConflictException;
import com.thanh.auction_server.exception.ResourceNotFoundException;
import com.thanh.auction_server.exception.UnauthorizedException;
import com.thanh.auction_server.exception.UserNotFoundException;
import com.thanh.auction_server.mapper.DisputeMapper;
import com.thanh.auction_server.mapper.InvoiceMapper;
import com.thanh.auction_server.repository.DisputeRepository;
import com.thanh.auction_server.repository.FeedbackRepository;
import com.thanh.auction_server.repository.InvoiceRepository;
import com.thanh.auction_server.repository.UserRepository;
import com.thanh.auction_server.service.auction.NotificationService;
import com.thanh.auction_server.service.authenticate.UserService;
import com.thanh.auction_server.service.payment.PaymentService;
import com.thanh.auction_server.specification.DisputeSpecification;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
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
    DisputeMapper disputeMapper;
    UserService userService;
    PaymentService paymentService;
    NotificationService notificationService;

    @Transactional
    public void createInvoiceForWinner(AuctionSession session, User winner) {

        // Chỉ chặn nếu đã tồn tại hóa đơn loại AUCTION_SALE cho phiên này
        // Không quan tâm nếu đã có hóa đơn LISTING_FEE
        if (invoiceRepository.existsByAuctionSessionIdAndType(session.getId(), InvoiceType.AUCTION_SALE)) {
            log.warn("Invoice Sale already exists for Auction Session ID: {}", session.getId());
            return;
        }
        LocalDateTime dueDate = LocalDateTime.now().plusDays(4);

        Invoice invoice = Invoice.builder()
                .user(winner)
                .auctionSession(session)
                .product(session.getProduct())
                .finalPrice(session.getCurrentPrice())
                .status(InvoiceStatus.PENDING)
                .type(InvoiceType.AUCTION_SALE)
                .createdAt(LocalDateTime.now())
                .dueDate(dueDate)
                .build();

        invoiceRepository.save(invoice);
        log.info("Invoice created (ID: {}) cho người thắng {} của phiên {}", invoice.getId(), winner.getUsername(), session.getId());

    }

    public PageResponse<InvoiceResponse> getMyInvoices(InvoiceStatus status, InvoiceType type, int page, int size) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User currentUser = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorMessage.USER_NOT_FOUND + username));
        Pageable pageable = PageRequest.of(page - 1, size);
        Page<Invoice> invoicePage = invoiceRepository.findByUserUsernameAndStatusAndType(username, status, type, pageable);
        List<InvoiceResponse> responses = invoicePage.getContent().stream().map(invoice -> {
            InvoiceResponse response = invoiceMapper.toInvoiceResponse(invoice);
        // check has been feedback
            boolean hasFeedback = feedbackRepository.existsByInvoiceIdAndFromUserId(invoice.getId(), currentUser.getId());
            response.setHasFeedback(hasFeedback);
            return response;
        }).toList();

        return PageResponse.<InvoiceResponse>builder()
                .currentPage(page)
                .totalPages(invoicePage.getTotalPages())
                .pageSize(invoicePage.getSize())
                .totalElements(invoicePage.getTotalElements())
                .data(responses)
                .build();
    }

    public PageResponse<InvoiceResponse> getInvoicesBySeller(InvoiceStatus status, int page, int size) {
        return getInvoicesForSeller(status, null, page, size); // null type = lấy tất cả
    }

    // API lấy đơn hàng bán ra (Doanh thu) -> AUCTION_SALE
    public PageResponse<InvoiceResponse> getMySales(InvoiceStatus status, int page, int size) {
        return getInvoicesForSeller(status, InvoiceType.AUCTION_SALE, page, size);
    }

    // API lấy hóa đơn phí sàn (Chi phí) -> LISTING_FEE
    public PageResponse<InvoiceResponse> getMyListingFees(InvoiceStatus status, int page, int size) {
        return getInvoicesForSeller(status, InvoiceType.LISTING_FEE, page, size);
    }

    public InvoiceResponse getInvoiceById(Long id) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        Invoice invoice = invoiceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorMessage.INVOICE_NOT_FOUND + id));

        boolean isBuyer = invoice.getUser().getUsername().equals(username);
        boolean isSeller = invoice.getProduct().getSeller().getUsername().equals(username);

        if (!isBuyer && !isSeller) {
            throw new UnauthorizedException(ErrorMessage.UNAUTHORIZED);
        }

        return invoiceMapper.toInvoiceResponse(invoice);
    }

    @Transactional
    public MessageResponse reportNonPayment(Long invoiceId) {
        String currentUsername = SecurityContextHolder.getContext().getAuthentication().getName();
        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorMessage.INVOICE_NOT_FOUND + invoiceId));
        User seller = invoice.getProduct().getSeller();
        User buyer = invoice.getUser();
        // Chỉ người bán mới có quyền báo cáo
        if (!seller.getUsername().equals(currentUsername)) {
            throw new UnauthorizedException(ErrorMessage.UNAUTHORIZED);
        }
        if (invoice.getStatus() != InvoiceStatus.PENDING) {
            throw new DataConflictException(ErrorMessage.STATUS_INCORRECT + invoice.getStatus());
        }
        //Kiểm tra hạn chót thanh toán (dueDate)
        if (invoice.getDueDate().isAfter(LocalDateTime.now())) {
            throw new DataConflictException("Bạn chỉ có thể báo cáo sau khi đã quá hạn thanh toán.");
        }
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

    private PageResponse<InvoiceResponse> getInvoicesForSeller(InvoiceStatus status, InvoiceType type, int page, int size) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        Pageable pageable = PageRequest.of(page - 1, size);
        Page<Invoice> invoicePage = invoiceRepository.findBySellerUsernameStatusAndType(username, status, type, pageable);

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
    //========================Díspute Section========================//
    @Transactional
    public MessageResponse resolveDispute(Long disputeId, ResolveDisputeRequest request) {
        Dispute dispute = disputeRepository.findById(disputeId)
                .orElseThrow(() -> new ResourceNotFoundException("Dispute not found"));
        Invoice invoice = dispute.getInvoice();
        if (invoice.getStatus() != InvoiceStatus.DISPUTE) {
            throw new DataConflictException("Hóa đơn này không trong trạng thái tranh chấp.");
        }
        // Xử lý theo quyết định của Admin
        if (request.getDecision() == DisputeDecision.REFUND_TO_BUYER) {
            // A. BUYER THẮNG
            log.info("Bắt đầu quy trình hoàn tiền cho Invoice ID: {}", invoice.getId());
            invoice.setStatus(InvoiceStatus.REFUNDED);
            dispute.setDecision(DisputeDecision.REFUND_TO_BUYER);
            try {
                paymentService.refundTransaction(invoice.getId());
                log.info("Gọi API hoàn tiền thành công");
            } catch (Exception e) {
                throw new RuntimeException("Không thể hoàn tiền qua VNPay: " + e.getMessage());
            }
            notificationService.createNotification(invoice.getUser(),
                    "Khiếu nại đơn hàng " + invoice.getProduct().getName() + " thành công. Bạn sẽ được hoàn tiền.",
                    "/invoices/" + invoice.getId());

            notificationService.createNotification(invoice.getProduct().getSeller(),
                    "Khiếu nại đơn hàng " + invoice.getProduct().getName() + ": Admin phán quyết Người mua thắng.",
                    "/invoices/" + invoice.getId());

        } else {
            // B. SELLER THẮNG
            invoice.setStatus(InvoiceStatus.COMPLETED);
            dispute.setDecision(DisputeDecision.RELEASE_TO_SELLER);
            notificationService.createNotification(invoice.getUser(),
                    "Khiếu nại đơn hàng " + invoice.getProduct().getName() + " bị từ chối. Giao dịch hoàn tất.",
                    "/invoices/" + invoice.getId());

            notificationService.createNotification(invoice.getProduct().getSeller(),
                    "Khiếu nại đơn hàng " + invoice.getProduct().getName() + ": Admin phán quyết Bạn thắng. Tiền đã được giải ngân.",
                    "/invoices/" + invoice.getId());
        }
        invoiceRepository.save(invoice);

        // Cập nhật trạng thái Dispute
        dispute.setAdminNote(request.getAdminNote());
        dispute.setResolvedAt(LocalDateTime.now());
        disputeRepository.save(dispute);

        return MessageResponse.builder()
                .message("Đã giải quyết khiếu nại thành công.")
                .build();
    }

    public PageResponse<DisputeResponse> getAllDisputes(DisputeSearchRequest request, int page, int size) {
        Sort sortObj = Sort.by("createdAt").descending();
        if (request.getSort() != null) {
            sortObj = switch (request.getSort().toLowerCase()) {
                case "oldest" -> Sort.by("createdAt").ascending();
                case "resolved_newest" -> Sort.by("resolvedAt").descending();
                case "resolved_oldest" -> Sort.by("resolvedAt").ascending();
                default -> Sort.by("createdAt").descending();
            };
        }
        Pageable pageable = PageRequest.of(page - 1, size, sortObj);

        Specification<Dispute> spec = DisputeSpecification.getFilter(request);
        Page<Dispute> disputePage = disputeRepository.findAll(spec, pageable);

        List<DisputeResponse> responses = disputePage.getContent().stream().map(dispute -> {
            return DisputeResponse.builder()
                    .id(dispute.getId())
                    .invoiceId(dispute.getInvoice().getId())
                    .reason(dispute.getReason())
                    .adminNote(dispute.getAdminNote())
                    .createdAt(dispute.getCreatedAt())
                    .resolvedAt(dispute.getResolvedAt())
                    .decision(dispute.getDecision())
                    .build();
        }).toList();
        return PageResponse.<DisputeResponse>builder()
                .currentPage(page)
                .pageSize(size)
                .totalPages(disputePage.getTotalPages())
                .totalElements(disputePage.getTotalElements())
                .data(responses)
                .build();
    }

    public DisputeResponse getDisputeByInvoiceId(Long invoiceId) {
        Dispute dispute = disputeRepository.findByInvoiceId(invoiceId)
                .orElseThrow(() -> new ResourceNotFoundException("Dispute not found for Invoice ID: " + invoiceId));
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User currentUser = userRepository.findByUsername(username)
                .orElseThrow(() -> new UserNotFoundException(ErrorMessage.USER_NOT_FOUND + username));

        String buyerUsername = dispute.getInvoice().getUser().getUsername();
        String sellerUsername = dispute.getInvoice().getProduct().getSeller().getUsername();
        boolean isBuyer = username.equals(buyerUsername);
        boolean isSeller = username.equals(sellerUsername);
        // Kiểm tra vai trò ADMIN co the them cacs role khacs
        boolean isAdmin = currentUser.getRoles().stream().anyMatch(r -> r.getName().equals("ADMIN"));
        if (!isBuyer && !isSeller && !isAdmin) {
            throw new UnauthorizedException(ErrorMessage.UNAUTHORIZED);
        }
        return disputeMapper.toDisputeResponse(dispute);
    }

    public InvoiceResponse adminGetInvoiceById(Long invoiceId) {
        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorMessage.INVOICE_NOT_FOUND + invoiceId));
        return invoiceMapper.toInvoiceResponse(invoice);
    }
}
