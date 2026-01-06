package com.thanh.auction_server.dto.response;

import com.thanh.auction_server.constants.DisputeDecision;
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
    DisputeDecision decision;
    String adminNote;
    LocalDateTime createdAt;
    LocalDateTime resolvedAt;
}
