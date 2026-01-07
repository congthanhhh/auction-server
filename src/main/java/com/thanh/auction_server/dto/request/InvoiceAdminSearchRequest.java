package com.thanh.auction_server.dto.request;

import com.thanh.auction_server.constants.AuctionStatus;
import com.thanh.auction_server.constants.InvoiceStatus;
import com.thanh.auction_server.constants.InvoiceType;
import lombok.Data;

@Data
public class InvoiceAdminSearchRequest {
    String keyword; // Tìm chung cho: ID Invoice, Username, Tên sản phẩm
    InvoiceStatus status;
    InvoiceType type;
    String sort;
}
