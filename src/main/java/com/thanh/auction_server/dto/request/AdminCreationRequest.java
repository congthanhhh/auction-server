package com.thanh.auction_server.dto.request;

import com.thanh.auction_server.validation.NoAccentsOrSpaces;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;
import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AdminCreationRequest {
    @NoAccentsOrSpaces
    String username;
    String password;
    String firstName;
    String lastName;
    String email;
    String phoneNumber;
    Boolean isActive;
    Set<String> roles;
    LocalDateTime createdAt = LocalDateTime.now();
}
