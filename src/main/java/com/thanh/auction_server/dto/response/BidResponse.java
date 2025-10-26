package com.thanh.auction_server.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class BidResponse {
    Long id;
    BigDecimal displayedAmount;
    LocalDateTime bidTime;
    SimpleUserResponse user;
    Long auctionSessionId;
}
