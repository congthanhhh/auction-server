package com.thanh.auction_server.dto.request;

import com.thanh.auction_server.constants.DisputeDecision;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ResolveDisputeRequest {
    @NotNull(message = "Vui lòng chọn quyết định")
    private DisputeDecision decision;
    private String adminNote;
}
