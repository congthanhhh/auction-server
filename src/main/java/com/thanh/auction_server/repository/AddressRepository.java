package com.thanh.auction_server.repository;

import com.thanh.auction_server.entity.Address;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AddressRepository extends JpaRepository<Address, Long> {
    List<Address> findByUser_Id(String userId);
    // Tìm địa chỉ mặc định của user
    Address findByUser_IdAndIsDefaultTrue(String userId);

}
