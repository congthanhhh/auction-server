package com.thanh.auction_server.dto.request;

import com.thanh.auction_server.constants.DisputeDecision;
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
    private String adminNote;
}
