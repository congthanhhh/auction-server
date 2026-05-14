package com.thanh.auction_server.dto.request;

import com.thanh.auction_server.constants.AuctionStatus;
import lombok.Data;

@Data
public class AuctionSessionAdminSearchRequest {
    String productName;
    AuctionStatus status;
    String sort;
}
