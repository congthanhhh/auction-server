package com.thanh.auction_server.dto.response;

import com.thanh.auction_server.constants.AuctionStatus;
import jakarta.validation.constraints.Future;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AuctionSessionResponse {
    Long id;
    LocalDateTime startTime;
    LocalDateTime endTime;
    BigDecimal startPrice;
    BigDecimal currentPrice;
    BigDecimal buyNowPrice;
    AuctionStatus status;
    SimpleProductResponse product;
    SimpleUserResponse highestBidder;
    boolean reservePriceMet;

    // Không trả về highestMaxBid (bí mật)

    // Tùy chọn: Thêm các thông tin khác như số lượng bid, số người tham gia...
    // private int bidCount;
    // private List<SimpleBidResponse> recentBids; // Ví dụ: 5 bid gần nhất
}
