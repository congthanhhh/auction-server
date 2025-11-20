package com.thanh.auction_server.dto.response;

import com.thanh.auction_server.constants.InvoiceStatus;
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
}
