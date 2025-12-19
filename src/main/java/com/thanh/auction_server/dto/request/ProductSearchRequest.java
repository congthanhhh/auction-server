package com.thanh.auction_server.dto.request;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class ProductSearchRequest {
    String keyword;
    Long categoryId;
    BigDecimal minPrice;
    BigDecimal maxPrice;
    // nhunwgx field khác
}
