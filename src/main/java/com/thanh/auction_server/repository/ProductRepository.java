package com.thanh.auction_server.repository;

import com.thanh.auction_server.constants.ProductStatus;
import com.thanh.auction_server.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long>, JpaSpecificationExecutor<Product> {

    Page<Product> findAllByIsActiveTrue(Pageable pageable);

    Optional<Product> findByIdAndIsActiveTrue(Long id);
    List<Product> findBySeller_Id(String sellerId);

    List<Product> findAllBySeller_UsernameAndIsActiveTrue(String username);

    @Query("SELECT p FROM Product p WHERE p.seller.username = :username AND p.isActive = true " +
            "AND p.id NOT IN (SELECT a.product.id FROM AuctionSession a)")
    List<Product> findAllBySeller_UsernameAndIsActiveTrueAndNotInAuctionSession(@Param("username") String username);

    Page<Product> findByStatus(ProductStatus status, Pageable pageable);
    Page<Product> findAllByIsActiveTrueAndStatus(ProductStatus status, Pageable pageable);

    long countByStatus(ProductStatus status);

}
