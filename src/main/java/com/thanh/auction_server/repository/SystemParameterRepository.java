package com.thanh.auction_server.repository;

import com.thanh.auction_server.entity.SystemParameter;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SystemParameterRepository extends JpaRepository<SystemParameter, Long> {
    boolean existsByParamKey(String paramKey);
    Optional<SystemParameter> findByParamKey(String paramKey);
}
