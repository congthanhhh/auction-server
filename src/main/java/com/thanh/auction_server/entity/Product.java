package com.thanh.auction_server.entity;

import com.thanh.auction_server.constants.ProductStatus;
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
public class Product {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @Column(nullable = false)
    String name;

    @Column(columnDefinition = "JSON")
    private String attributes;

    @Lob
    @Column(columnDefinition = "TEXT")
    String description;

    @Column(nullable = false)
    BigDecimal startPrice;

    LocalDateTime createdAt;

    @Builder.Default
    @Column(columnDefinition = "BOOLEAN DEFAULT TRUE")
    Boolean isActive = true;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    Category category;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seller_id", nullable = false)
    User seller;

    // Một Product có nhiều Image
    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id")
    Set<Image> images;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    ProductStatus status;
}
