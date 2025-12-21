package com.thanh.auction_server.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class DisputeRequest {
    @NotBlank(message = "Vui lòng nhập lý do khiếu nại")
    String reason;
}
