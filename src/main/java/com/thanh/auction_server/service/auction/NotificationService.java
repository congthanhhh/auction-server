package com.thanh.auction_server.service.auction;

import com.thanh.auction_server.constants.ErrorMessage;
import com.thanh.auction_server.dto.request.NotificationRequest;
import com.thanh.auction_server.dto.response.NotificationResponse;
import com.thanh.auction_server.dto.response.PageResponse;
import com.thanh.auction_server.entity.Notification;
import com.thanh.auction_server.entity.User;
import com.thanh.auction_server.exception.ResourceNotFoundException;
import com.thanh.auction_server.exception.UnauthorizedException;
import com.thanh.auction_server.exception.UserNotFoundException;
import com.thanh.auction_server.mapper.NotificationMapper;
import com.thanh.auction_server.repository.NotificationRepository;
import com.thanh.auction_server.repository.UserRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    NotificationMapper notificationMapper;
    // private final SocketIOService socketIOService; // Sẽ inject sau

    @Transactional
    public void createNotification(User user, String message, String link) {
        Notification notification = Notification.builder()
                .user(user)
                .message(message)
                .link(link)
                .isRead(false)
                .createdAt(LocalDateTime.now())
                .build();

        notificationRepository.save(notification);
        log.info("Notification saved for user {}: {}", user.getUsername(), message);

        // TODO (Sau Bước 2): Gọi WebSocket
        // String userRoom = "user-" + user.getId();
        // socketIOService.sendMessageToRoom(userRoom, "new_notification", notificationMapper.toResponse(notification));
    }

    public PageResponse<NotificationResponse> getMyNotifications(int page, int size) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UserNotFoundException(ErrorMessage.USER_NOT_FOUND));

        Pageable pageable = PageRequest.of(page - 1, size);
        Page<Notification> notificationPage = notificationRepository.findByUser_IdOrderByCreatedAtDesc(user.getId(), pageable);

        return PageResponse.<NotificationResponse>builder()
                .currentPage(page)
                .totalPages(notificationPage.getTotalPages())
                .pageSize(notificationPage.getSize())
                .totalElements(notificationPage.getTotalElements())
                .data(notificationPage.getContent().stream()
                        .map(notificationMapper::toNotificationResponse)
                        .toList())
                .build();
    }

    public long getUnreadCount() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorMessage.USER_NOT_FOUND));
        return notificationRepository.countByUser_IdAndIsReadFalse(user.getId());
    }

    @Transactional
    public NotificationResponse markAsRead(Long notificationId) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found"));

        if (!notification.getUser().getUsername().equals(username)) {
            throw new UnauthorizedException("Bạn không có quyền xem thông báo này.");
        }

        notification.setIsRead(true);
        Notification savedNotification = notificationRepository.save(notification);
        return notificationMapper.toNotificationResponse(savedNotification);
    }

}
