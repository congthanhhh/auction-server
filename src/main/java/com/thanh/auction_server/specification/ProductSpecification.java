package com.thanh.auction_server.specification;

import com.thanh.auction_server.constants.AuctionStatus;
import com.thanh.auction_server.dto.request.ProductSearchRequest;
import com.thanh.auction_server.entity.AuctionSession;
import com.thanh.auction_server.entity.Product;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

public class ProductSpecification {

    public static Specification<Product> getFilter(ProductSearchRequest request) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (StringUtils.hasText(request.getKeyword())) {
                String keyword = "%" + request.getKeyword().toLowerCase() + "%";
                // 1. Tìm theo tên
                Predicate namePredicate = criteriaBuilder.like(criteriaBuilder.lower(root.get("name")), keyword);
                Predicate descPredicate = criteriaBuilder.like(
                        criteriaBuilder.lower(root.get("description").as(String.class)),
                        keyword
                );
                predicates.add(criteriaBuilder.or(namePredicate, descPredicate));
            }
            // Lọc theo CATEGORY
            if (request.getCategoryId() != null) {
                predicates.add(criteriaBuilder.equal(root.get("category").get("id"), request.getCategoryId()));
            }

            // Lọc theo KHOẢNG GIÁ (Starting Price)
            if (request.getMinPrice() != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("startPrice"), request.getMinPrice()));
            }
            if (request.getMaxPrice() != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("startPrice"), request.getMaxPrice()));
            }
            // Lọc theo SELLER (Cho Admin hoặc trang Profile của user)
            if (StringUtils.hasText(request.getSellerId())) {
                predicates.add(criteriaBuilder.equal(root.get("seller").get("id"), request.getSellerId()));
            }

            // Lọc theo STATUS
            if (request.getStatus() != null) {
                predicates.add(criteriaBuilder.equal(root.get("status"), request.getStatus()));
            }
            // 6. Lọc theo isActive
            if (request.getIsActive() != null) {
                predicates.add(criteriaBuilder.equal(root.get("isActive"), request.getIsActive()));
            }
            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }
}
