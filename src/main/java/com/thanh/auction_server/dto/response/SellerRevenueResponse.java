        package com.thanh.auction_server.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SellerRevenueResponse {
    long totalAuctionSessions; // Tổng số phiên đấu giá đã tạo
    Long totalRevenue;         // Tổng doanh thu (VNĐ)
}
