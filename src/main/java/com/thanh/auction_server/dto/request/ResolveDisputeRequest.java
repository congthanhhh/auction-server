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
public class ResolveDisputeRequest {
    @NotNull(message = "Vui lòng chọn quyết định")
    private DisputeDecision decision;

    public enum DisputeDecision {
        REFUND_TO_BUYER, // Buyer thắng -> Hoàn tiền
        RELEASE_TO_SELLER // Seller thắng -> Chuyển tiền (Hoàn tất đơn)
    }

    private String adminNote;
}
