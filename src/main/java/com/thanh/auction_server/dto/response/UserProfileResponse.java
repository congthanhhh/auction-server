package com.thanh.auction_server.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Data
@Builder
public class UserProfileResponse {
    String id;
    String username;
    String firstName;
    String lastName;
    String email;
    String phoneNumber;
    boolean noPassword;
    Boolean isActive;
    Integer strikeCount;
    Integer reputationScore;
    LocalDateTime createdAt;
    Set<RoleResponse> roles;
}
