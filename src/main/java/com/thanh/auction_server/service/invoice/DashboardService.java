package com.thanh.auction_server.service.invoice;

import com.thanh.auction_server.constants.InvoiceStatus;
import com.thanh.auction_server.dto.response.SellerRevenueResponse;
import com.thanh.auction_server.repository.AuctionSessionRepository;
import com.thanh.auction_server.repository.InvoiceRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;

@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
@Service
public class DashboardService {
    AuctionSessionRepository auctionSessionRepository;
    InvoiceRepository invoiceRepository;

    public SellerRevenueResponse getSellerDashboard() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();

        long totalSessions = auctionSessionRepository.countBySellerUsername(username);

        List<InvoiceStatus> revenueStatuses = List.of(
//                InvoiceStatus.PAID,
                InvoiceStatus.COMPLETED
        );
        Long totalRevenue = invoiceRepository.sumRevenueBySellerAndStatus(username, revenueStatuses);

        return SellerRevenueResponse.builder()
                .totalAuctionSessions(totalSessions)
                .totalRevenue(totalRevenue)
                .build();
    }
}
