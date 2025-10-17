//package com.thanh.auction_server.entity;
//
//import jakarta.persistence.*;
//import lombok.*;
//import lombok.experimental.FieldDefaults;
//
//import java.math.BigDecimal;
//import java.util.Set;
//
//@Getter
//@Setter
//@Builder
//@NoArgsConstructor
//@AllArgsConstructor
//@FieldDefaults(level = AccessLevel.PRIVATE)
//@Entity
//public class Product {
//    @Id
//    @GeneratedValue(strategy = GenerationType.IDENTITY)
//    Long id;
//
//    @Column(nullable = false)
//    String name;
//
//    @Lob // Dùng cho các chuỗi dài
//    String description;
//
//    @Column(nullable = false)
//    BigDecimal startPrice; // Giá khởi điểm, dùng BigDecimal cho tiền tệ
//
//    // Một Product thuộc về một Category
//    @ManyToOne(fetch = FetchType.LAZY)
//    @JoinColumn(name = "category_id")
//    Category category;
//
//    // Một Product được đăng bởi một User (người bán)
//    @ManyToOne(fetch = FetchType.LAZY)
//    @JoinColumn(name = "seller_id")
//    User seller;
//
//    // Một Product có nhiều Image
//    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
//    @JoinColumn(name = "product_id")
//    Set<Image> images;
//}
