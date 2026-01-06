package com.thanh.auction_server.entity;

import com.thanh.auction_server.constants.DisputeDecision;
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
public class Dispute {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invoice_id", nullable = false, unique = true)
    Invoice invoice;

    @Column(nullable = false, columnDefinition = "TEXT")
    String reason;
    @Enumerated(EnumType.STRING)
    DisputeDecision decision = DisputeDecision.PENDING;
    @Column(columnDefinition = "TEXT")
    String adminNote;
    LocalDateTime createdAt;
    LocalDateTime resolvedAt;
}
