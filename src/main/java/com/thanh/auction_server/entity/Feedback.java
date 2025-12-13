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
public class Feedback {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    // Người viết đánh giá (Sender)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "from_user_id", nullable = false)
    User fromUser;

    // Người nhận đánh giá (Receiver) - Điểm uy tín của người này sẽ bị ảnh hưởng
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "to_user_id", nullable = false)
    User toUser;

    // Gắn liền với hóa đơn (Invoice) cụ thể
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invoice_id", nullable = false)
    Invoice invoice;

    @Column(nullable = false)
    Integer rating; // 1 đến 5 sao (eBay dùng Pos/Neg, nhưng 1-5 sao linh hoạt hơn và hiện đại hơn)

    @Column(columnDefinition = "TEXT")
    String comment;

    LocalDateTime createdAt;
}
