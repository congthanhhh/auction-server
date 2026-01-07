package com.thanh.auction_server.dto.request;

import com.thanh.auction_server.constants.InvoiceStatus;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AdminUpdateInvoiceRequest {
    InvoiceStatus status;
    String trackingCode;
    String carrier;
    String recipientName;
    String recipientPhone;
    String shippingAddress;
    String note;
}
