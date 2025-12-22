package com.thanh.auction_server.mapper;

import com.thanh.auction_server.dto.request.AddressRequest;
import com.thanh.auction_server.dto.response.AddressResponse;
import com.thanh.auction_server.entity.Address;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface AddressMapper {
    Address toAddress(AddressRequest request);

    @Mapping(target = "fullAddress", expression = "java(getFullAddress(address))")
    AddressResponse toAddressResponse(Address address);

    void updateAddress(@MappingTarget Address address, AddressRequest request);

    default String getFullAddress(Address address) {
        return String.format("%s, %s, %s, %s",
                address.getStreet(), address.getWard(), address.getDistrict(), address.getCity());
    }
}
