package com.thanh.auction_server.dto.request;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UserUpdateRequest {
    String username;
    String fullName;
    String password;
    String email;
    LocalDateTime updateAt = LocalDateTime.now();
    List<String> roles;
}
