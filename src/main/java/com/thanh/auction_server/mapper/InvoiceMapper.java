package com.thanh.auction_server.mapper;

import com.thanh.auction_server.dto.response.InvoiceResponse;
import com.thanh.auction_server.entity.Invoice;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = {UserMapper.class, ProductMapper.class})
public interface InvoiceMapper {

    @Mapping(source = "user", target = "user")
    @Mapping(source = "product", target = "product")
    @Mapping(source = "auctionSession.id", target = "auctionSessionId")
    InvoiceResponse toInvoiceResponse(Invoice invoice);


}
