package com.thanh.auction_server.Controller;

import com.thanh.auction_server.constants.InvoiceStatus;
import com.thanh.auction_server.constants.InvoiceType;
import com.thanh.auction_server.dto.request.*;
import com.thanh.auction_server.dto.response.*;
import com.thanh.auction_server.service.invoice.DashboardService;
import com.thanh.auction_server.service.invoice.InvoiceService;
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
    private final DashboardService dashboardService;

    @GetMapping("/my-invoices")
    public ResponseEntity<PageResponse<InvoiceResponse>> getMyInvoices(
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "size", defaultValue = "10") int size,
            @RequestParam(value = "status", required = false) InvoiceStatus status,
            @RequestParam(value = "type", required = false) InvoiceType type) {
        PageResponse<InvoiceResponse> response = invoiceService.getMyInvoices(status, type, page, size);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/my-sales")
    public ResponseEntity<PageResponse<InvoiceResponse>> getMySales(
            @RequestParam(value = "status", required = false) InvoiceStatus status,
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "size", defaultValue = "10") int size) {

        return ResponseEntity.ok(invoiceService.getMySales(status, page, size));
    }

    @GetMapping("/my-listing-fees")
    public ResponseEntity<PageResponse<InvoiceResponse>> getMyListingFees(
            @RequestParam(value = "status", required = false) InvoiceStatus status,
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "size", defaultValue = "10") int size) {

        return ResponseEntity.ok(invoiceService.getMyListingFees(status, page, size));
    }

    @GetMapping("/sold-invoices")
    public ResponseEntity<PageResponse<InvoiceResponse>> getAllSellerInvoices(
            @RequestParam(value = "status", required = false) InvoiceStatus status,
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "size", defaultValue = "10") int size) {
        return ResponseEntity.ok(invoiceService.getInvoicesBySeller(status, page, size));
    }

    @GetMapping("/{id}")
    public ResponseEntity<InvoiceResponse> getInvoiceById(@PathVariable Long id) {
        InvoiceResponse response = invoiceService.getInvoiceById(id);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/report-nonpayment")
    public ResponseEntity<MessageResponse> reportNonPayment(@PathVariable Long id) {
        MessageResponse response = invoiceService.reportNonPayment(id);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/ship")
    public ResponseEntity<MessageResponse> shipInvoice(
            @PathVariable Long id,
            @RequestBody @Validated ShipInvoiceRequest request) {
        return ResponseEntity.ok(invoiceService.shipInvoice(id, request));
    }

    @PostMapping("/{id}/confirm")
    public ResponseEntity<MessageResponse> confirmInvoice(@PathVariable Long id) {
        return ResponseEntity.ok(invoiceService.confirmInvoice(id));
    }

    @GetMapping("/dispute/{invoiceId}")
    public ResponseEntity<DisputeResponse> getDisputeByInvoice(@PathVariable Long invoiceId) {
        return ResponseEntity.ok(invoiceService.getDisputeByInvoiceId(invoiceId));
    }

    @GetMapping("/seller-stats")
    public ResponseEntity<SellerRevenueResponse> getSellerStats() {
        return ResponseEntity.ok(dashboardService.getSellerDashboard());
    }

    @PostMapping("/{id}/dispute")
    public ResponseEntity<MessageResponse> reportDispute(
            @PathVariable Long id,
            @RequestBody @Validated DisputeRequest request) {
        return ResponseEntity.ok(invoiceService.reportDispute(id, request));
    }

    @GetMapping("/admin/invoice/{invoiceId}")
    public ResponseEntity<InvoiceResponse> getInvoiceByIdForAdmin(@PathVariable Long invoiceId) {
        InvoiceResponse response = invoiceService.adminGetInvoiceById(invoiceId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/admin/disputes/{id}/resolve")
    public ResponseEntity<MessageResponse> resolveDispute(
            @PathVariable Long id,
            @RequestBody ResolveDisputeRequest request) {
        return ResponseEntity.ok(invoiceService.resolveDispute(id, request));
    }
    @GetMapping("/admin/disputes")
    public ResponseEntity<PageResponse<DisputeResponse>> getDisputes(
            @ModelAttribute DisputeSearchRequest request,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(invoiceService.getAllDisputes(request, page, size));
    }

    @PutMapping("/admin/update/{id}")
    public ResponseEntity<InvoiceResponse> updateInvoiceForAdmin(
            @PathVariable Long id,
            @RequestBody AdminUpdateInvoiceRequest request
    ) {
        return ResponseEntity.ok(invoiceService.updateInvoiceForAdmin(id, request));
    }

    @GetMapping("/admin/search")
    public ResponseEntity<PageResponse<InvoiceResponse>> getAllInvoicesForAdmin(
            @ModelAttribute InvoiceAdminSearchRequest request,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ResponseEntity.ok(invoiceService.getAllInvoicesForAdmin(request, page, size));
    }

}