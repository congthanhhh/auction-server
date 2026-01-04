package com.thanh.auction_server.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
    public class DisputeResponse {
    Long id;
    Long invoiceId;
    String reason;
    String adminNote;
    LocalDateTime createdAt;
    LocalDateTime resolvedAt;
}
