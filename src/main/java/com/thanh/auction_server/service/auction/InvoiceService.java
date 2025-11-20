package com.thanh.auction_server.service.auction;

import com.thanh.auction_server.constants.ErrorMessage;
import com.thanh.auction_server.constants.InvoiceStatus;
import com.thanh.auction_server.dto.response.InvoiceResponse;
import com.thanh.auction_server.dto.response.MessageResponse;
import com.thanh.auction_server.dto.response.PageResponse;
import com.thanh.auction_server.entity.AuctionSession;
import com.thanh.auction_server.entity.Invoice;
import com.thanh.auction_server.entity.User;
import com.thanh.auction_server.exception.DataConflictException;
import com.thanh.auction_server.exception.ResourceNotFoundException;
import com.thanh.auction_server.exception.UnauthorizedException;
import com.thanh.auction_server.mapper.InvoiceMapper;
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
    UserRepository userRepository;
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

        // 5a. Hủy hóa đơn
        invoice.setStatus(InvoiceStatus.CANCELLED_NON_PAYMENT);
        invoiceRepository.save(invoice);
        log.info("Hóa đơn ID {} đã bị hủy do không thanh toán.", invoiceId);

        // 5b. Tăng điểm phạt cho người mua
        userService.incrementStrikeCount(buyer.getId()); // Gọi hàm đã tạo ở bước trước

        // 5c. Gửi thông báo "bell icon" cho người mua
        String message = String.format("Bạn đã nhận 1 điểm phạt vì không thanh toán hóa đơn cho sản phẩm '%s'.",
                invoice.getProduct().getName());
        notificationService.createNotification(buyer, message, "/my-profile/strikes"); // Link tới trang điểm phạt (ví dụ)

        return MessageResponse.builder()
                .message("Báo cáo không thanh toán thành công. Người mua đã bị 1 điểm phạt.")
                .build();
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
