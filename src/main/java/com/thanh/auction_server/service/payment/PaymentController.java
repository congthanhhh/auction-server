package com.thanh.auction_server.service.payment;

import com.thanh.auction_server.dto.response.MessageResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/payments")
@RequiredArgsConstructor
public class PaymentController {
    private final PaymentService paymentService;

    //Tạo URL thanh toán
    @GetMapping("/vn-pay")
    public ResponseEntity<String> createPayment(
            HttpServletRequest request,
            @RequestParam Long invoiceId,
            @RequestParam Long addressId
    ) {
        String paymentUrl = paymentService.createVnPayPayment(request, invoiceId, addressId);
        return ResponseEntity.ok(paymentUrl);
    }
    //Xử lý callback từ VNPay
    @GetMapping("/vn-pay-callback")
    public ResponseEntity<PaymentResponse> handleVnPayCallback(HttpServletRequest request) {
        PaymentResponse status = paymentService.handleVnPayCallback(request);
        return ResponseEntity.ok(status);
    }
}
