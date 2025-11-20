package com.thanh.auction_server.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;
import java.util.Set;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Entity
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    String id;
    String username;
    String firstName;
    String lastName;
    String password;
    String email;
    Boolean isActive = false;
    LocalDateTime createdAt;
    LocalDateTime updatedAt;

    @Builder.Default
    @Column(columnDefinition = "INT DEFAULT 0")
    Integer strikeCount = 0;

    @ManyToMany
    Set<Role> roles;
}
