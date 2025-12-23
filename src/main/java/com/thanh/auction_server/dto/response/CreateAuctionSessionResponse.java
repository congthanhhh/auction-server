package com.thanh.auction_server.dto.response;

import com.thanh.auction_server.constants.AuctionStatus;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CreateAuctionSessionResponse {
    String message;
    String paymentUrl;
    AuctionSessionResponse sessionDetails;
}
