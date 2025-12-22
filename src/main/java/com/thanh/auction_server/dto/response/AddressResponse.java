package com.thanh.auction_server.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AddressResponse {
    Long id;
    String recipientName;
    String phoneNumber;
    String street;
    String ward;
    String district;
    String city;
    Boolean isDefault;
    String fullAddress;
}
