package com.thanh.auction_server.repository;

import com.thanh.auction_server.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoleRepository extends JpaRepository<Role, String> {
}
