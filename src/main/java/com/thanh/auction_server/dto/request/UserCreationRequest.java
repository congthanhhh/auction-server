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
    String firstName;
    String lastName;
//    @Size(min = 6, message = "Password must be at least 6 characters long")
    String password;
    String email;
    String phoneNumber;
    Boolean isActive = false;
    LocalDateTime createdAt = LocalDateTime.now();
}
