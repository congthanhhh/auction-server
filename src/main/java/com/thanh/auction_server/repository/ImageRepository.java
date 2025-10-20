package com.thanh.auction_server.repository;

import com.thanh.auction_server.entity.Image;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ImageRepository extends JpaRepository<Image, Integer> {
}
