package com.thanh.auction_server.dto.request;

import com.thanh.auction_server.constants.AuctionStatus;
import com.thanh.auction_server.constants.ProductStatus;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class AuctionSessionAdminSearchRequest {
    String productName;
    AuctionStatus status;
    String sort;
}
