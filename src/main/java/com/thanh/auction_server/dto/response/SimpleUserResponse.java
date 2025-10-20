package com.thanh.auction_server.dto.response;

import com.thanh.auction_server.entity.Image;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SimpleUserResponse {
    String username;
    String firstName;
    String lastName;
    String email;
}
