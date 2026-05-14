package com.thanh.auction_server.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class BidRequest {
    @NotNull(message = "Money amount must not be null")
    @Positive(message = "Money amount must be positive")
    BigDecimal amount;
}
