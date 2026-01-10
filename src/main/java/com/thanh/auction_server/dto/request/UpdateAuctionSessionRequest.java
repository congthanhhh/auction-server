package com.thanh.auction_server.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UpdateAuctionSessionRequest {
    LocalDateTime startTime;
    LocalDateTime endTime;
    BigDecimal startPrice;
    BigDecimal reservePrice;
    BigDecimal buyNowPrice;
}
