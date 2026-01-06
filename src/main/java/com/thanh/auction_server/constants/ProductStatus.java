package com.thanh.auction_server.constants;

public enum ProductStatus {
    WAITING_FOR_APPROVAL, // Chờ admin duyệt
    ACTIVE,               // Đã duyệt, được phép lên sàn
    REJECTED,             // Bị từ chối
    BANNED                // Bị khóa sau này
}
