package com.thanh.auction_server.service.invoice;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class InvoiceScheduler {
    private final InvoiceService invoiceService;
    // Chạy mỗi ngày một lần vào lúc 00:00 đêm
    // Cron expression: "giây phút giờ ngày tháng tuần"
    @Scheduled(cron = "0 0 0 * * *")
//     @Scheduled(fixedRate = 60000) // Chạy mỗi 1 phút để test
    public void autoFinishInvoicesJob() {
        log.info("Scheduler: Bắt đầu quét các đơn hàng shipping quá hạn...");
        try {
            invoiceService.autoFinishInvoices();
        } catch (Exception e) {
            log.error("Scheduler: Lỗi trong quá trình auto-finish invoices", e);
        }
    }
}
