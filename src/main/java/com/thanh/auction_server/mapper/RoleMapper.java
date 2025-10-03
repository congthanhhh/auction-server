package com.thanh.auction_server.mapper;

import com.thanh.auction_server.dto.request.RoleRequest;
import com.thanh.auction_server.dto.response.RoleResponse;
import com.thanh.auction_server.entity.Role;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface RoleMapper {

    @Mapping(target = "permissions", ignore = true)
    Role toRole(RoleRequest request);

    RoleResponse toRoleResponse(Role role);
}
