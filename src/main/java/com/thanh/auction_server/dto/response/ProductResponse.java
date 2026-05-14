package com.thanh.auction_server.dto.response;

import com.thanh.auction_server.constants.ProductStatus;
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
public class ProductResponse {
    Long id;
    String name;
    String description;
    BigDecimal startPrice;
    LocalDateTime createdAt;
    CategoryResponse category;
    SimpleUserResponse seller;
    ProductStatus status;
    String attributes;
    Boolean isActive;
    Set<Image> images;
}
