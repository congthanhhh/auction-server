package com.thanh.auction_server.constants;

public enum InvoiceStatus {
    PENDING,                // Đang chờ thanh toán
    PAID,                   // Đã thanh toán (Chờ gửi hàng)
    SHIPPING,               // Đã gửi hàng (Chờ nhận hàng)
    COMPLETED,              // Đã hoàn thành (Thành công) -> Kích hoạt Feedback
    DISPUTE,                // Đang khiếu nại (Buyer báo chưa nhận được hàng)
    CANCELLED_NON_PAYMENT,  // Hủy do bùng hàng
    CANCELLED_BY_SELLER,     // Hủy bởi người bán
    REFUNDED
}
