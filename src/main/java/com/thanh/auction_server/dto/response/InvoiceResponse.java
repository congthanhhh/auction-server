package com.thanh.auction_server.dto.response;

import com.thanh.auction_server.constants.InvoiceStatus;
import com.thanh.auction_server.constants.InvoiceType;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class InvoiceResponse {
    Long id;

    SimpleUserResponse user;
    SimpleProductResponse product;
    Long auctionSessionId;

    BigDecimal finalPrice;
    InvoiceStatus status;
    LocalDateTime createdAt;
    LocalDateTime dueDate;
    InvoiceType type;

    String shippingAddress;
    String recipientName;
    String recipientPhone;
    String trackingCode;
    String carrier;
    LocalDateTime shippedAt;
    LocalDateTime paymentTime;
}
