package com.thanh.auction_server.mapper;

import com.thanh.auction_server.dto.request.AdminCreationRequest;
import com.thanh.auction_server.dto.request.AdminUpdateRequest;
import com.thanh.auction_server.dto.request.UserCreationRequest;
import com.thanh.auction_server.dto.request.UserUpdateRequest;
import com.thanh.auction_server.dto.response.*;
import com.thanh.auction_server.entity.Product;
import com.thanh.auction_server.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.Named;

@Mapper(componentModel = "spring")
public interface UserMapper {

    User toUser(UserCreationRequest request);

    UserResponse toUserResponse(User user);

    void updateUser(@MappingTarget User user, UserUpdateRequest request);

    @Named("userToSimpleUserResponse")
    SimpleUserResponse userToSimpleUserResponse(User user);

    UserProfileResponse toUserProfileResponse(User user);

    PublicUserProfileResponse toPublicUserProfileResponse(User user);

    @Mapping(target = "roles", ignore = true) // Sẽ xử lý roles thủ công trong Service
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "strikeCount", ignore = true) // Mặc định = 0
    @Mapping(target = "reputationScore", ignore = true) // Mặc định = 0
    User toUserFromAdminCreate(AdminCreationRequest request);

    // Mapping cho Admin Update
    @Mapping(target = "roles", ignore = true)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "username", ignore = true) // Thường username không cho đổi
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "password", ignore = true) // Xử lý password riêng
    void updateUserFromAdminRequest(@MappingTarget User user, AdminUpdateRequest request);
}
