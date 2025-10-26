package com.thanh.auction_server.entity;

import com.thanh.auction_server.constants.AuctionStatus;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Set;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Entity
public class AuctionSession {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    LocalDateTime startTime;
    LocalDateTime endTime;

    @Column(nullable = false, precision = 19, scale = 2)
    BigDecimal startPrice;

    @Column(precision = 19, scale = 2)
    BigDecimal currentPrice;

    @Column(precision = 19, scale = 2)
    BigDecimal reservePrice;

    @Column(precision = 19, scale = 2)
    BigDecimal buyNowPrice;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    AuctionStatus status;

    //Proxy Bidding
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "highest_bidder_id")
    User highestBidder;
    @Column(precision = 19, scale = 2)
    BigDecimal highestMaxBid;
    // End Proxy Bidding

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", referencedColumnName = "id", unique = true, nullable = false)
    Product product;

    @OneToMany(mappedBy = "auctionSession", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    Set<Bid> bids;

    LocalDateTime createdAt;
    LocalDateTime updatedAt;
}
