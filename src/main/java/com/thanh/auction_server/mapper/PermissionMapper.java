package com.thanh.auction_server.mapper;

import com.thanh.auction_server.dto.request.PermissionRequest;
import com.thanh.auction_server.dto.response.PermissionResponse;
import com.thanh.auction_server.entity.Permission;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface PermissionMapper {

    Permission toPermission(PermissionRequest request);

    PermissionResponse topermissionResponse(Permission permission);
}
