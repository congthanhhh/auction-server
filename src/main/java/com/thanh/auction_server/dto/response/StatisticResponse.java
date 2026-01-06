package com.thanh.auction_server.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class StatisticResponse {
    long totalUsers;          // Tổng số thành viên
    long activeAuctions;      // Số phiên đấu giá đang chạy
    long pendingProducts;     // Số sản phẩm chờ duyệt
    BigDecimal totalRevenue;  // Tổng doanh thu thực tế (Net Revenue)
    BigDecimal totalGMV;      // Tổng giá trị giao dịch (Gross Merchandise Value)
    BigDecimal totalListingFee; // Tổng phí niêm yết (Listing Fee)
    BigDecimal commissionRevenue; // Doanh thu từ hoa hồng (Commission Revenue)
}
