package com.thanh.auction_server.repository;

import com.thanh.auction_server.entity.Dispute;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

public interface DisputeRepository extends JpaRepository<Dispute, Long>, JpaSpecificationExecutor<Dispute> {
    Optional<Dispute> findByInvoiceId(Long invoiceId);
}
