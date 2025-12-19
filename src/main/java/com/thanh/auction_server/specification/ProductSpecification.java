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

            // 2. Lọc theo CATEGORY
            if (request.getCategoryId() != null) {
                // Giả sử Product có quan hệ @ManyToOne với Category tên là "category"
                predicates.add(criteriaBuilder.equal(root.get("category").get("id"), request.getCategoryId()));
            }

            // 3. Lọc theo KHOẢNG GIÁ (Starting Price)
            if (request.getMinPrice() != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("startPrice"), request.getMinPrice()));
            }
            if (request.getMaxPrice() != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("startPrice"), request.getMaxPrice()));
            }

            // 4. (QUAN TRỌNG) Chỉ lấy sản phẩm đang ACTIVE (Đang đấu giá)
            // Logic này tùy thuộc vào DB của bạn.
            // Nếu Product không có status mà phụ thuộc vào AuctionSession, bạn cần Join bảng.
            // Dưới đây là ví dụ đơn giản nếu Product có field status hoặc isActive:
            // predicates.add(criteriaBuilder.equal(root.get("isActive"), true));

            // *NẾU* bạn muốn lọc dựa trên AuctionSession Status (ACTIVE):
            // Join<Product, AuctionSession> sessionJoin = root.join("auctionSessions", JoinType.LEFT);
            // predicates.add(criteriaBuilder.equal(sessionJoin.get("status"), AuctionStatus.ACTIVE));

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }
}
