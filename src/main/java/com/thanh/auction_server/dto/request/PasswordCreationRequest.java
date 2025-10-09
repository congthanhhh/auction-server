package com.thanh.auction_server.dto.request;

import com.thanh.auction_server.validation.NoAccentsOrSpaces;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PasswordCreationRequest {
//    @Size(min = 6, message = "Password must be at least 6 characters long")
    String password;
}
