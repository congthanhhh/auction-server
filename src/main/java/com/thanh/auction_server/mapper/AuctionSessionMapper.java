package com.thanh.auction_server.mapper;

import com.thanh.auction_server.dto.request.AuctionSessionRequest;
import com.thanh.auction_server.dto.response.AuctionSessionResponse;
import com.thanh.auction_server.dto.response.SimpleProductResponse;
import com.thanh.auction_server.dto.response.SimpleUserResponse;
import com.thanh.auction_server.entity.AuctionSession;
import com.thanh.auction_server.entity.Product;
import com.thanh.auction_server.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring", uses = {UserMapper.class, ProductMapper.class})
public interface AuctionSessionMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "product", ignore = true) // Sẽ set thủ công từ productId
    @Mapping(target = "startPrice", ignore = true) // Sẽ lấy từ Product
    @Mapping(target = "currentPrice", ignore = true) // Sẽ set bằng startPrice ban đầu
    @Mapping(target = "status", ignore = true) // Sẽ set là SCHEDULED ban đầu
    @Mapping(target = "highestBidder", ignore = true)
    @Mapping(target = "highestMaxBid", ignore = true)
    @Mapping(target = "bids", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    AuctionSession toAuctionSession(AuctionSessionRequest request);

    @Mapping(source = "product", target = "product", qualifiedByName = "productToSimpleProductResponse") // Cần hàm map Product -> SimpleProductResponse
    @Mapping(source = "highestBidder", target = "highestBidder", qualifiedByName = "userToSimpleUserResponse") // Cần hàm map User -> SimpleUserResponse
    AuctionSessionResponse toAuctionSessionResponse(AuctionSession auctionSession);


    // Map để cập nhật (ít dùng cho session, chủ yếu là cập nhật status, price)
    // void updateAuctionSession(@MappingTarget AuctionSession session, AuctionSessionRequest request);

}
