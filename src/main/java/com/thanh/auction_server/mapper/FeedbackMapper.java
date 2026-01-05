package com.thanh.auction_server.mapper;

import com.thanh.auction_server.dto.response.FeedbackDto;
import com.thanh.auction_server.entity.Feedback;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface FeedbackMapper {
    @Mapping(target = "fromUsername", source = "fromUser.username")
    @Mapping(target = "toUsername", source = "toUser.username")
    FeedbackDto toFeedbackDto(Feedback feedback);
}
