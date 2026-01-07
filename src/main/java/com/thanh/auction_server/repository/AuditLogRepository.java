package com.thanh.auction_server.repository;

import com.thanh.auction_server.entity.AuditLog;
import com.thanh.auction_server.entity.SystemParameter;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
    Page<AuditLog> findByAdminUsernameContainingOrActionContaining(String username, String action, Pageable pageable);
}
