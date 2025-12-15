package com.thanh.auction_server.dto.response;

import com.thanh.auction_server.constants.AuctionStatus;
import jakarta.validation.constraints.Future;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AuctionSessionResponse {
    Long id;
    LocalDateTime startTime;
    LocalDateTime endTime;
    BigDecimal startPrice;
    BigDecimal currentPrice;
    BigDecimal buyNowPrice;
    AuctionStatus status;
    SimpleProductResponse product;
    SimpleUserResponse highestBidder;
    boolean reservePriceMet;
    BigDecimal myMaxBid;
}
