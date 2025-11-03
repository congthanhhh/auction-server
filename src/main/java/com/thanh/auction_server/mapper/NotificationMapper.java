package com.thanh.auction_server.mapper;

import com.thanh.auction_server.dto.request.NotificationRequest;
import com.thanh.auction_server.dto.response.NotificationResponse;
import com.thanh.auction_server.entity.Notification;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface NotificationMapper {

    Notification toNotification(NotificationRequest request);

    NotificationResponse toNotificationResponse(Notification notification);

    void updateNotification(@MappingTarget Notification notification, NotificationRequest request);
}
