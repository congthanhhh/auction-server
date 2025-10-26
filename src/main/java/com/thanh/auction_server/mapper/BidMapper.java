package com.thanh.auction_server.mapper;

import com.thanh.auction_server.dto.request.BidRequest;
import com.thanh.auction_server.dto.response.BidResponse;
import com.thanh.auction_server.dto.response.SimpleUserResponse;
import com.thanh.auction_server.entity.Bid;
import com.thanh.auction_server.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = {UserMapper.class})
public interface BidMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "bidTime", ignore = true) // Sẽ set thủ công là now()
    @Mapping(target = "user", ignore = true) // Sẽ lấy từ SecurityContext
    @Mapping(target = "auctionSession", ignore = true) // Sẽ lấy từ path variable hoặc context
    // @Mapping(target = "priceWhenBid", ignore = true) // Nếu có
    Bid toBid(BidRequest request);

    // Map từ Entity sang Response
    @Mapping(source = "user", target = "user") // Cần hàm map User -> SimpleUserResponse
    @Mapping(source = "auctionSession.id", target = "auctionSessionId") // Lấy ID của session
    @Mapping(target = "displayedAmount", ignore = true) // Không map amount trực tiếp, cần xử lý logic
    BidResponse toBidResponse(Bid bid);

    // Hàm trợ giúp map User -> SimpleUserResponse (Có thể dùng lại)
    SimpleUserResponse userToSimpleUserResponse(User user);
}
