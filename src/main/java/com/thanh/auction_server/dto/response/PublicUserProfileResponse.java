package com.thanh.auction_server.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PublicUserProfileResponse {
    String id;
    String username;
    String firstName;
    String lastName;
    Integer reputationScore;
    LocalDateTime createdAt;
}
