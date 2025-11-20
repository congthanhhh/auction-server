package com.thanh.auction_server.Controller;

import com.thanh.auction_server.dto.request.*;
import com.thanh.auction_server.dto.response.InvoiceResponse;
import com.thanh.auction_server.dto.response.MessageResponse;
import com.thanh.auction_server.dto.response.PageResponse;
import com.thanh.auction_server.dto.response.UserResponse;
import com.thanh.auction_server.service.auction.InvoiceService;
import com.thanh.auction_server.service.authenticate.UserService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RequiredArgsConstructor
@RestController
@RequestMapping("/invoices")
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class InvoiceController {

    InvoiceService invoiceService;

    @GetMapping("/my-invoices")
    public ResponseEntity<PageResponse<InvoiceResponse>> getMyInvoices(
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "size", defaultValue = "10") int size) {
        PageResponse<InvoiceResponse> response = invoiceService.getMyInvoices(page, size);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<InvoiceResponse> getInvoiceById(@PathVariable Long id) {
        InvoiceResponse response = invoiceService.getInvoiceById(id);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/pay")
    public ResponseEntity<String> createPaymentForInvoice(@PathVariable Long id) {

        return ResponseEntity.ok("Chức năng thanh toán sẽ được implement sau.");
    }

    @PostMapping("/{id}/report-nonpayment")
    public ResponseEntity<MessageResponse> reportNonPayment(@PathVariable Long id) {
        MessageResponse response = invoiceService.reportNonPayment(id);
        return ResponseEntity.ok(response);
    }
}