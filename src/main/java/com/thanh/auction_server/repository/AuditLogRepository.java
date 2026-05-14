package com.thanh.auction_server.repository;

import com.thanh.auction_server.entity.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
    Page<AuditLog> findByAdminUsernameContainingOrActionContaining(String username, String action, Pageable pageable);
}
