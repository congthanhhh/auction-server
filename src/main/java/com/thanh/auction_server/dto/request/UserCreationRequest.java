package com.thanh.auction_server.dto.request;

import com.thanh.auction_server.validation.NoAccentsOrSpaces;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UserCreationRequest {
    @NoAccentsOrSpaces
    String username;
    String fullName;
    String password;
    String email;
    LocalDateTime createdAt = LocalDateTime.now();
}
