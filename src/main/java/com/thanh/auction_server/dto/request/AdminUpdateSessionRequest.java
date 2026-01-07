package com.thanh.auction_server.dto.request;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.thanh.auction_server.constants.AuctionStatus;
import jakarta.validation.constraints.Future;
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
public class AdminUpdateSessionRequest {
    @Future(message = "Start time must be in the future")
    LocalDateTime startTime;

    @Future(message = "End time must be in the future")
    LocalDateTime endTime;

    BigDecimal startPrice;
    BigDecimal reservePrice;
    BigDecimal buyNowPrice;

    AuctionStatus status;
}
