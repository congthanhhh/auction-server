package com.thanh.auction_server.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class UserProfileResponse {

    String id;
    String username;
    String firstName;
    String lastName;
    String email;
    String phoneNumber;

    Integer reputationScore; // Điểm uy tín (+15, -2...)
    Integer totalFeedbacks;  // Tổng số lượt đánh giá

    List<FeedbackDto> recentFeedbacks; // 5 đánh giá gần nhất
    List<ProductResponse> products;    // Các sản phẩm đang bán (dùng lại ProductResponse cũ)
}
