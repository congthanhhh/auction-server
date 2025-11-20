package com.thanh.auction_server.constants;

public enum InvoiceStatus {
    PENDING, // Đang chờ thanh toán (mới tạo)
    PAID,    // Đã thanh toán thành công
    CANCELLED_NON_PAYMENT, // Đã hủy do người mua không thanh toán
    CANCELLED_BY_SELLER // Đã hủy bởi người bán (lý do khác)
}
