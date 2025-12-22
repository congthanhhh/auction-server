package com.thanh.auction_server.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Entity
public class Address {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    User user;

    @Column(nullable = false)
    String recipientName;

    @Column(nullable = false)
    String phoneNumber;

    String street;
    String ward;
    String district;
    String city;

    @Builder.Default
    @Column(columnDefinition = "BOOLEAN DEFAULT FALSE")
    Boolean isDefault = false;
}
