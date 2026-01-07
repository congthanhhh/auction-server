package com.thanh.auction_server.repository;

import com.thanh.auction_server.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, String> {

    boolean existsByUsername(String username);

    Optional<User> findByUsername(String username);
    Optional<User> findByEmail(String email);
    Optional<User> findByUsernameOrEmail(String username,String email);
    boolean existsByPhoneNumber(String phoneNumber);
    boolean existsByEmail(String email);

    @Query("SELECT DISTINCT u FROM User u " +
            "LEFT JOIN u.roles r " +
            "WHERE (:isActive IS NULL OR u.isActive = :isActive) " +
            "AND (:roleName IS NULL OR r.name = :roleName)")
    Page<User> searchUsers(
            @Param("isActive") Boolean isActive,
            @Param("roleName") String roleName,
            Pageable pageable);

    List<User> findByRoles_Name(String roleName);
}
