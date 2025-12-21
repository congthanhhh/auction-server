package com.thanh.auction_server.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ShipInvoiceRequest {

    @NotBlank(message = "Vui lòng nhập mã vận đơn")
    String trackingCode;

    @NotBlank(message = "Vui lòng nhập tên đơn vị vận chuyển")
    String carrier;
}
