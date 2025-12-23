package com.thanh.auction_server.constants;

public enum AuctionStatus {
    SCHEDULED, // Phiên đã được tạo nhưng chưa tới giờ bắt đầu
    ACTIVE,    // Phiên đang diễn ra, cho phép đặt giá
    ENDED,     // Phiên đã kết thúc (hết giờ)
    CANCELLED, // Phiên bị hủy (ví dụ: do không đủ người quan tâm, hoặc admin hủy)
    PENDING_PAYMENT, // Đã kết thúc và có người thắng, chờ thanh toán
    COMPLETED, // Đã kết thúc, người thắng đã thanh toán
    FAILED,     // Đã kết thúc nhưng không có người thắng hợp lệ (không ai bid hoặc không đạt giá sàn)
    WAITING_PAYMENT
}
