package com.thanh.auction_server.service.payment;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PaymentResponse {
    String code;      // "00": Thành công, Khác: Thất bại
    String message;
    String paymentTime;
    String transactionId;
    String invoiceId;
}
