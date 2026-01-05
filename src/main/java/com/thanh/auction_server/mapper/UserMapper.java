package com.thanh.auction_server.mapper;

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

    @Mapping(target = "roles", ignore = true)
    void updateUser(@MappingTarget User user, UserUpdateRequest request);

    @Named("userToSimpleUserResponse")
    SimpleUserResponse userToSimpleUserResponse(User user);

    UserProfileResponse toUserProfileResponse(User user);

    PublicUserProfileResponse toPublicUserProfileResponse(User user);
}
