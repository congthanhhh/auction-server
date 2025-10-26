package com.thanh.auction_server.dto.request;

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
public class AuctionSessionRequest {
    Long productId;

    @Future(message = "Start time must be in the future")
    LocalDateTime startTime;

    @Future(message = "End time must be in the future")
    LocalDateTime endTime;

    BigDecimal reservePrice;
    BigDecimal buyNowPrice;
}
