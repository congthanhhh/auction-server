package com.thanh.auction_server.entity;

import com.thanh.auction_server.constants.InvoiceStatus;
import com.thanh.auction_server.constants.InvoiceType;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Entity
public class Invoice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    User user;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "auction_session_id", nullable = false, unique = true)
    AuctionSession auctionSession;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    Product product;

    @Column(nullable = false, precision = 19, scale = 2)
    BigDecimal finalPrice;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    InvoiceStatus status;

    String shippingAddress;
    String recipientName;
    String recipientPhone;

    @Column(name = "tracking_code")
    String trackingCode; // Mã vận đơn

    @Column(name = "carrier")
    String carrier;      // Đơn vị vận chuyển (Ví dụ: GHTK, Viettel Post)

    @Column(name = "shipped_at")
    LocalDateTime shippedAt;

    @Column(nullable = false)
    LocalDateTime createdAt;

    @Column(nullable = false)
    LocalDateTime dueDate;

    @Enumerated(EnumType.STRING)
    InvoiceType type;

    @Column(name = "payment_time")
    private String paymentTime;
}
