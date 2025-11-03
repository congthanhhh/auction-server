package com.thanh.auction_server.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Entity
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    User user;

    @Column(nullable = false)
    String message;

    @Builder.Default
    @Column(columnDefinition = "BOOLEAN DEFAULT FALSE")
    Boolean isRead = false;

    // (Rất quan trọng) Link để khi click vào sẽ điều hướng
    String link; // Ví dụ: "/auctions/123"

    @Column(nullable = false)
    LocalDateTime createdAt;
}
