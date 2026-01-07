package com.thanh.auction_server.specification;

import com.thanh.auction_server.dto.request.InvoiceAdminSearchRequest;
import com.thanh.auction_server.entity.Invoice;
import com.thanh.auction_server.entity.Product;
import com.thanh.auction_server.entity.User;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
public class InvoiceSpecification {
    public static Specification<Invoice> getFilter(InvoiceAdminSearchRequest request) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // 1. TÌM KIẾM TỪ KHÓA (KEYWORD)
            if (StringUtils.hasText(request.getKeyword())) {
                String keyword = request.getKeyword().trim().toLowerCase();
                List<Predicate> keywordPredicates = new ArrayList<>();
                // a. Tìm theo ID (Nếu keyword là số)
                if (keyword.matches("\\d+")) {
                    keywordPredicates.add(cb.equal(root.get("id"), Long.parseLong(keyword)));
                }
                // b. Tìm theo Username (Join User)
                Join<Invoice, User> userJoin = root.join("user");
                keywordPredicates.add(cb.like(cb.lower(userJoin.get("username")), "%" + keyword + "%"));

                // c. Tìm theo Tên sản phẩm (Join Product)
                Join<Invoice, Product> productJoin = root.join("product");
                keywordPredicates.add(cb.like(cb.lower(productJoin.get("name")), "%" + keyword + "%"));

                // Dùng OR cho nhóm này: (ID = ... OR User LIKE ... OR Product LIKE ...)
                predicates.add(cb.or(keywordPredicates.toArray(new Predicate[0])));
            }
            // 2. LỌC THEO TRẠNG THÁI
            if (request.getStatus() != null) {
                predicates.add(cb.equal(root.get("status"), request.getStatus()));
            }
            // 3. LỌC THEO LOẠI
            if (request.getType() != null) {
                predicates.add(cb.equal(root.get("type"), request.getType()));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
