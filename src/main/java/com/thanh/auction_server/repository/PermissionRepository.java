package com.thanh.auction_server.repository;

import com.thanh.auction_server.entity.Permission;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PermissionRepository extends JpaRepository<Permission, String> {
}
