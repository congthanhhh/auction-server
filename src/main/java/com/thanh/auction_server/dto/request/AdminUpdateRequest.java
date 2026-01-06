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
public class AdminUpdateRequest {
    String password;
    String firstName;
    String lastName;
    String email;
    String phoneNumber;
    Boolean isActive;

    Integer strikeCount;
    Integer reputationScore;

    Set<String> roles;
    LocalDateTime updatedAt = LocalDateTime.now();
}
