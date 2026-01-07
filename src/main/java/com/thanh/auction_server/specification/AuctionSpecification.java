package com.thanh.auction_server.specification;

import com.thanh.auction_server.dto.request.AuctionSessionAdminSearchRequest;
import com.thanh.auction_server.entity.AuctionSession;
import com.thanh.auction_server.entity.Product;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

public class AuctionSpecification {
    public static Specification<AuctionSession> getFilter(AuctionSessionAdminSearchRequest request) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // 1. Tìm theo tên sản phẩm (Join với bảng Product)
            if (StringUtils.hasText(request.getProductName())) {
                Join<AuctionSession, Product> productJoin = root.join("product");
                predicates.add(cb.like(cb.lower(productJoin.get("name")), "%" + request.getProductName().toLowerCase() + "%"));
            }
            // 2. Filter theo status
            if (request.getStatus() != null) {
                predicates.add(cb.equal(root.get("status"), request.getStatus()));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
