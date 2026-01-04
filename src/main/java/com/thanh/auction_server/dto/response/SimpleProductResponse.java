package com.thanh.auction_server.dto.response;

import com.thanh.auction_server.constants.AuctionStatus;
import com.thanh.auction_server.entity.Image;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SimpleProductResponse {
    Long id;
    String name;
    SimpleUserResponse seller;
    BigDecimal startPrice;
    Set<Image> images;
}
