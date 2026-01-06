package com.thanh.auction_server.constants;

public enum DisputeDecision {
    PENDING,
    REFUND_TO_BUYER, // Buyer thắng -> Hoàn tiền
    RELEASE_TO_SELLER // Seller thắng -> Hoàn tất
    ;

}
