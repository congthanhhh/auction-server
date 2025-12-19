package com.thanh.auction_server.dto.request;

import com.thanh.auction_server.constants.FeedbackRating;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class FeedbackRequest {

    @NotNull(message = "Vui lòng chọn đánh giá: POSITIVE, NEUTRAL hoặc NEGATIVE")
    FeedbackRating rating;

    String comment;
}
