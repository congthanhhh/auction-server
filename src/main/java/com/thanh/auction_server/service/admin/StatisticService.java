package com.thanh.auction_server.service.admin;

import com.thanh.auction_server.constants.AuctionStatus;
import com.thanh.auction_server.constants.ProductStatus;
import com.thanh.auction_server.dto.response.StatisticResponse;
import com.thanh.auction_server.repository.AuctionSessionRepository;
import com.thanh.auction_server.repository.InvoiceRepository;
import com.thanh.auction_server.repository.ProductRepository;
import com.thanh.auction_server.repository.UserRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;

@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
@Service
public class StatisticService {
    InvoiceRepository invoiceRepository;
    UserRepository userRepository;
    ProductRepository productRepository;
    AuctionSessionRepository auctionSessionRepository;

    private static final BigDecimal COMMISSION_RATE = new BigDecimal("0.10");

    public StatisticResponse getDashboardStatistics(Integer month, Integer year) {
        LocalDateTime startDate = null;
        LocalDateTime endDate = null;
        if (year != null) {
            if (month != null) {
                // Lọc theo Tháng cụ thể trong Năm
                YearMonth yearMonth = YearMonth.of(year, month);
                startDate = yearMonth.atDay(1).atStartOfDay(); // Ngày 1 lúc 00:00:00
                endDate = yearMonth.atEndOfMonth().atTime(LocalTime.MAX); // Ngày cuối lúc 23:59:59
            } else {
                // Lọc theo cả Năm (Từ 1/1 đến 31/12)
                startDate = LocalDateTime.of(year, 1, 1, 0, 0);
                endDate = LocalDateTime.of(year, 12, 31, 23, 59, 59);
            }
        }
        // Thống kê người dùng, phiên đấu giá, sản phẩm chờ duyệt
        long totalUsers = userRepository.count();
        long activeAuctions = auctionSessionRepository.countByStatus(AuctionStatus.ACTIVE);
        long pendingProducts = productRepository.countByStatus(ProductStatus.WAITING_FOR_APPROVAL);
        // Tính toán DOANH THU THEO KHOẢNG THỜI GIAN
        // a. Phí niêm yết (giá sàn)
        BigDecimal totalListingFee = invoiceRepository.sumListingFeeBetween(startDate, endDate);
        if (totalListingFee == null) totalListingFee = BigDecimal.ZERO;
        // b. Tổng Giá trị giao dịch (GMV)
        BigDecimal totalSales = invoiceRepository.sumAuctionSalesBetween(startDate, endDate);
        if (totalSales == null) totalSales = BigDecimal.ZERO;
        // c. Hoa hồng
        BigDecimal commissionRevenue = totalSales.multiply(COMMISSION_RATE);

        // d. Tổng doanh thu
        BigDecimal netRevenue = totalListingFee.add(commissionRevenue);

        return StatisticResponse.builder()
                .totalUsers(totalUsers)
                .activeAuctions(activeAuctions)
                .pendingProducts(pendingProducts)
                .totalRevenue(netRevenue)
                .totalGMV(totalSales)
                .totalListingFee(totalListingFee)
                .commissionRevenue(commissionRevenue)
                .build();
    }

}
