package com.thanh.auction_server.repository;

import com.thanh.auction_server.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long> {

    Page<Product> findAllByIsActiveTrue(Pageable pageable);

    Optional<Product> findByIdAndIsActiveTrue(Long id);
}
