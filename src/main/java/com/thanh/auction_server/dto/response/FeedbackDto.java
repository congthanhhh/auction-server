package com.thanh.auction_server.dto.response;

import com.thanh.auction_server.constants.FeedbackRating;
import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
public class FeedbackDto {
    Long id;
    String fromUsername;
    String toUsername;
    FeedbackRating rating;
    String comment;
    LocalDateTime createdAt;
    String reviewAs;
}
