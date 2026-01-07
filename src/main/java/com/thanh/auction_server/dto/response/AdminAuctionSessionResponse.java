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
public class AdminAuctionSessionResponse {
    Long id;
    LocalDateTime startTime;
    LocalDateTime endTime;
    BigDecimal startPrice;
    BigDecimal currentPrice;
    BigDecimal reservePrice;
    BigDecimal buyNowPrice;
    BigDecimal highestMaxBid;
    AuctionStatus status;
    SimpleProductResponse product;
    SimpleUserResponse highestBidder;
    LocalDateTime createdAt;
    LocalDateTime updatedAt;
}
