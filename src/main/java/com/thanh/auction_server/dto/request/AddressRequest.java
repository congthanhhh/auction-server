package com.thanh.auction_server.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AddressRequest {
    @NotBlank(message = "Tên người nhận không được để trống")
    String recipientName;

    @NotBlank(message = "Số điện thoại không được để trống")
    String phoneNumber;

    @NotBlank(message = "Địa chỉ chi tiết không được để trống")
    String street;

    @NotBlank(message = "Phường/Xã không được để trống")
    String ward;

    @NotBlank(message = "Quận/Huyện không được để trống")
    String district;

    @NotBlank(message = "Tỉnh/Thành phố không được để trống")
    String city;

    Boolean isDefault;
}
