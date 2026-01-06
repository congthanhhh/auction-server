package com.thanh.auction_server.dto.request;

import com.thanh.auction_server.constants.ProductStatus;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class ProductSearchRequest {
    String keyword;
    Long categoryId;
    BigDecimal minPrice;
    BigDecimal maxPrice;
    String sort;

    ProductStatus status;
    String sellerId;
    Boolean isActive;
}
