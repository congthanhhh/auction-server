package com.thanh.auction_server.mapper;

import com.thanh.auction_server.dto.response.DisputeResponse;
import com.thanh.auction_server.entity.Dispute;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface DisputeMapper {
    @Mapping(source = "invoice.id", target = "invoiceId")
    DisputeResponse toDisputeResponse(Dispute dispute);
}
