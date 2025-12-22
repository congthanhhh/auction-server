package com.thanh.auction_server.service.payment;

import com.thanh.auction_server.constants.ErrorMessage;
import com.thanh.auction_server.constants.InvoiceStatus;
import com.thanh.auction_server.entity.Address;
import com.thanh.auction_server.entity.Invoice;
import com.thanh.auction_server.exception.ResourceNotFoundException;
import com.thanh.auction_server.exception.UnauthorizedException;
import com.thanh.auction_server.repository.AddressRepository;
import com.thanh.auction_server.repository.InvoiceRepository;
import com.thanh.auction_server.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {
    private final VnPayConfig vnPayConfig;
    private final InvoiceRepository invoiceRepository;
    private final AddressRepository addressRepository;
    private final UserRepository userRepository;

    // 1. TẠO URL THANH TOÁN
    @Transactional
    public String createVnPayPayment(HttpServletRequest request, Long invoiceId, Long addressId) {
        String currentUsername = SecurityContextHolder.getContext().getAuthentication().getName();

        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorMessage.INVOICE_NOT_FOUND));
        if (!invoice.getUser().getUsername().equals(currentUsername)) {
            throw new UnauthorizedException(ErrorMessage.UNAUTHORIZED);
        }
        if (invoice.getStatus() != InvoiceStatus.PENDING) {
            throw new RuntimeException(ErrorMessage.STATUS_INCORRECT);
        }
        // Snapshot Địa chỉ (Lưu cứng vào hóa đơn)
        Address address = addressRepository.findById(addressId)
                .orElseThrow(() -> new ResourceNotFoundException("Address not found"));
        invoice.setShippingAddress(address.getStreet() + ", " + address.getWard() + ", " + address.getDistrict() + ", " + address.getCity());
        invoice.setRecipientName(address.getRecipientName());
        invoice.setRecipientPhone(address.getPhoneNumber());
        invoiceRepository.save(invoice);

        // Build tham số gửi sang VNPay
        long amount = (long) (invoice.getFinalPrice().doubleValue() * 100); // VNPay yêu cầu nhân 100 (VND)
        String vnp_TxnRef = String.valueOf(invoice.getId()) + "_" + vnPayConfig.getRandomNumber(8); // Mã đơn hàng (Duy nhất)
        //vnp_TxnRef nên là "ID_Random" để tránh trùng nếu thanh toán lại
        Map<String, String> vnp_Params = new HashMap<>();
        vnp_Params.put("vnp_Version", vnPayConfig.getVnp_Version());
        vnp_Params.put("vnp_Command", vnPayConfig.getVnp_Command());
        vnp_Params.put("vnp_TmnCode", vnPayConfig.getVnp_TmnCode());
        vnp_Params.put("vnp_Amount", String.valueOf(amount));
        vnp_Params.put("vnp_CurrCode", "VND");
        vnp_Params.put("vnp_BankCode", "NCB"); // Demo Sandbox chọn ngân hàng NCB
        vnp_Params.put("vnp_TxnRef", vnp_TxnRef);
        vnp_Params.put("vnp_OrderInfo", "Thanh toan hoa don #" + invoice.getId());
        vnp_Params.put("vnp_OrderType", vnPayConfig.getOrderType());
        vnp_Params.put("vnp_Locale", "vn");
        vnp_Params.put("vnp_ReturnUrl", vnPayConfig.getVnp_ReturnUrl());
        vnp_Params.put("vnp_IpAddr", vnPayConfig.getIpAddress(request));

        Calendar cld = Calendar.getInstance(TimeZone.getTimeZone("Etc/GMT+7"));
        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss");
        String vnp_CreateDate = formatter.format(cld.getTime());
        vnp_Params.put("vnp_CreateDate", vnp_CreateDate);
        cld.add(Calendar.MINUTE, 15); // Hết hạn sau 15p
        String vnp_ExpireDate = formatter.format(cld.getTime());
        vnp_Params.put("vnp_ExpireDate", vnp_ExpireDate);

        // Build chuỗi URL & Checksum
        List<String> fieldNames = new ArrayList<>(vnp_Params.keySet());
        Collections.sort(fieldNames);
        StringBuilder hashData = new StringBuilder();
        StringBuilder query = new StringBuilder();
        Iterator<String> itr = fieldNames.iterator();
        while (itr.hasNext()) {
            String fieldName = itr.next();
            String fieldValue = vnp_Params.get(fieldName);
            if ((fieldValue != null) && (fieldValue.length() > 0)) {
                // Build hash data
                hashData.append(fieldName);
                hashData.append('=');
                try {
                    hashData.append(URLEncoder.encode(fieldValue, StandardCharsets.US_ASCII.toString()));
                    // Build query
                    query.append(URLEncoder.encode(fieldName, StandardCharsets.US_ASCII.toString()));
                    query.append('=');
                    query.append(URLEncoder.encode(fieldValue, StandardCharsets.US_ASCII.toString()));
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
                if (itr.hasNext()) {
                    query.append('&');
                    hashData.append('&');
                }
            }
        }

        String queryUrl = query.toString();
        String vnp_SecureHash = vnPayConfig.hmacSHA512(vnPayConfig.getSecretKey(), hashData.toString());
        queryUrl += "&vnp_SecureHash=" + vnp_SecureHash;

        return vnPayConfig.getVnp_PayUrl() + "?" + queryUrl;
    }

    @Transactional
    public PaymentResponse handleVnPayCallback(HttpServletRequest request) {
        Map<String, String> fields = new HashMap<>();
        for (Enumeration<String> params = request.getParameterNames(); params.hasMoreElements(); ) {
            String fieldName = params.nextElement();
            String fieldValue = request.getParameter(fieldName);
            if ((fieldValue != null) && (fieldValue.length() > 0)) {
                fields.put(fieldName, fieldValue);
            }
        }
        String vnp_SecureHash = request.getParameter("vnp_SecureHash");
        if (fields.containsKey("vnp_SecureHashType")) {
            fields.remove("vnp_SecureHashType");
        }
        if (fields.containsKey("vnp_SecureHash")) {
            fields.remove("vnp_SecureHash");
        }
        // Checksum
        String signValue = vnPayConfig.hmacSHA512(vnPayConfig.getSecretKey(), buildHashData(fields));
        if (signValue.equals(vnp_SecureHash)) {
            // Hash hợp lệ -> Kiểm tra trạng thái giao dịch
            if ("00".equals(request.getParameter("vnp_ResponseCode"))) {
                // Giao dịch THÀNH CÔNG
                String vnp_TxnRef = request.getParameter("vnp_TxnRef");
                // Tách lấy Invoice ID (Vì lúc tạo lưu là invoiceId + "_" + random)
                String[] parts = vnp_TxnRef.split("_");
                Long invoiceId = Long.parseLong(parts[0]);

                Invoice invoice = invoiceRepository.findById(invoiceId)
                        .orElseThrow(() -> new ResourceNotFoundException("Invoice not found"));
                // Kiểm tra số tiền (Optional: Chống hack sửa amount trên URL)
                long vnp_Amount = Long.parseLong(request.getParameter("vnp_Amount")) / 100;
                if (vnp_Amount != invoice.getFinalPrice().longValue()) {
                    return PaymentResponse.builder().code("99").message("Invalid Amount").build();
                }
                if (invoice.getStatus() == InvoiceStatus.PENDING) {
                    invoice.setStatus(InvoiceStatus.PAID);
                    // invoice.setPaymentTime(LocalDateTime.now());
                    invoiceRepository.save(invoice);
                    log.info("Invoice {} payment successful", invoiceId);
                }
                return PaymentResponse.builder()
                        .code("00")
                        .message("Payment successful")
                        .transactionId(vnp_TxnRef)
                        .invoiceId(String.valueOf(invoiceId))
                        .paymentTime(request.getParameter("vnp_PayDate"))
                        .build();
            } else {
                // Giao dịch THẤT BẠI hoặc BỊ HỦY
                return PaymentResponse.builder()
                        .code("99")
                        .message("Payment failed or canceled by user")
                        .build();
            }
        } else {
            // Hash KHÔNG hợp lệ
            return PaymentResponse.builder()
                    .code("97")
                    .message("Invalid Checksum")
                    .build();
        }
    }

    // Hàm phụ trợ để build chuỗi hash (giống hàm create)
    private String buildHashData(Map<String, String> fields) {
        List<String> fieldNames = new ArrayList<>(fields.keySet());
        Collections.sort(fieldNames);
        StringBuilder hashData = new StringBuilder();
        Iterator<String> itr = fieldNames.iterator();
        while (itr.hasNext()) {
            String fieldName = itr.next();
            String fieldValue = fields.get(fieldName);
            if ((fieldValue != null) && (fieldValue.length() > 0)) {
                // Build hash data
                hashData.append(fieldName);
                hashData.append('=');
                try {
                    hashData.append(URLEncoder.encode(fieldValue, StandardCharsets.US_ASCII.toString()));
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
                if (itr.hasNext()) {
                    hashData.append('&');
                }
            }
        }
        return hashData.toString();
    }
}
