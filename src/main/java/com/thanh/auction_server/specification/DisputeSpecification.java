package com.thanh.auction_server.specification;

import com.thanh.auction_server.constants.DisputeDecision;
import com.thanh.auction_server.dto.request.DisputeSearchRequest;
import com.thanh.auction_server.entity.Dispute;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

public class DisputeSpecification {
    public static Specification<Dispute> getFilter(DisputeSearchRequest request) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (request.getDecision() != null) {
                String filter = request.getDecision().toUpperCase();
                if ("PENDING".equals(filter)) {
                    predicates.add(criteriaBuilder.equal(root.get("decision"), DisputeDecision.PENDING));
                } else if ("RESOLVED".equals(filter)) {
                    predicates.add(root.get("decision").in(
                            DisputeDecision.REFUND_TO_BUYER,
                            DisputeDecision.RELEASE_TO_SELLER
                    ));
                }
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }
}
