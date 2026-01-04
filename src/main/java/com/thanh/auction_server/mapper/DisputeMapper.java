package com.thanh.auction_server.mapper;

import com.thanh.auction_server.dto.request.AddressRequest;
import com.thanh.auction_server.dto.response.AddressResponse;
import com.thanh.auction_server.dto.response.DisputeResponse;
import com.thanh.auction_server.entity.Address;
import com.thanh.auction_server.entity.Dispute;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface DisputeMapper {
    @Mapping(source = "invoice.id", target = "invoiceId")
    DisputeResponse toDisputeResponse(Dispute dispute);
}
