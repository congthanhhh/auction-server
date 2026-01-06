package com.thanh.auction_server.dto.request;

import com.thanh.auction_server.constants.DisputeDecision;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DisputeSearchRequest {
    private String decision;
    private String sort;
}
