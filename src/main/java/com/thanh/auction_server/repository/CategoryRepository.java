package com.thanh.auction_server.repository;

import com.thanh.auction_server.entity.Category;
import com.thanh.auction_server.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CategoryRepository extends JpaRepository<Category, Long> {
    Optional<Category> findByName(String name);
    boolean existsByName(String name);

}
