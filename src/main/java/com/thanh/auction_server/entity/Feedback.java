package com.thanh.auction_server.entity;

import com.thanh.auction_server.constants.FeedbackRating;
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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "from_user_id", nullable = false)
    User fromUser;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "to_user_id", nullable = false)
    User toUser;

    // Gắn liền với hóa đơn (Invoice) cụ thể
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invoice_id", nullable = false)
    Invoice invoice;

    @Enumerated(EnumType.STRING) // Lưu dưới dạng chuỗi "POSITIVE", "NEGATIVE" trong DB
    @Column(nullable = false)
    FeedbackRating rating;

    @Column(columnDefinition = "TEXT")
    String comment;

    LocalDateTime createdAt;
}
