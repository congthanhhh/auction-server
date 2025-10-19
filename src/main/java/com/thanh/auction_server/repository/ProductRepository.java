package com.thanh.auction_server.repository;

import com.thanh.auction_server.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductRepository extends JpaRepository<Product, Long> {
}
