package com.thanh.auction_server.service.utils;

import com.corundumstudio.socketio.SocketIOServer;
import com.nimbusds.jwt.SignedJWT;
import com.thanh.auction_server.entity.User;
import com.thanh.auction_server.repository.UserRepository;
import jakarta.annotation.PostConstruct;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.text.ParseException;

@Service
@RequiredArgsConstructor
@Slf4j
public class SocketIOService {

    private final SocketIOServer server;
    private final UserRepository userRepository;

    // Danh sách các sự kiện
    public static final String EVENT_JOIN_SESSION_ROOM = "join_auction_session";
    public static final String EVENT_LEAVE_SESSION_ROOM = "leave_auction_session";
    public static final String EVENT_JOIN_USER_ROOM = "join_user_room";

    public static final String EVENT_PRICE_UPDATE = "price_update";
    public static final String EVENT_NEW_BID = "new_bid";
    public static final String EVENT_AUCTION_ENDED = "auction_ended";
    public static final String EVENT_STATUS_CHANGE = "status_change";
    public static final String EVENT_NEW_NOTIFICATION = "new_notification";

    @PostConstruct
    private void addEventListeners() {

        server.addConnectListener(client -> {
            log.info("Socket.IO Client đã kết nối: {}", client.getSessionId());
            // Tại đây bạn có thể lấy token và log ra user nào vừa kết nối nếu muốn
        });
        server.addDisconnectListener(client -> {
            log.info("Socket.IO Client đã ngắt kết nối: {}", client.getSessionId());
        });

        // Join Room Phiên đấu giá (Công khai)
        server.addEventListener(EVENT_JOIN_SESSION_ROOM, String.class, (client, roomName, ackRequest) -> {
            client.joinRoom(roomName);
            log.info("Client {} join room phiên: {}", client.getSessionId(), roomName);
        });

        // Leave Room Phiên đấu giá
        server.addEventListener(EVENT_LEAVE_SESSION_ROOM, String.class, (client, roomName, ackRequest) -> {
            client.leaveRoom(roomName);
            log.info("Client {} leave room phiên: {}", client.getSessionId(), roomName);
        });

        // Join Room Cá nhân (Bảo mật - Tự động xác định user từ Token)
        server.addEventListener(EVENT_JOIN_USER_ROOM, String.class, (client, roomName, ackRequest) -> {
            try {
                String token = client.getHandshakeData().getSingleUrlParam("token");
                if (token != null) {
                    SignedJWT signedJWT = SignedJWT.parse(token);
                    String username = signedJWT.getJWTClaimsSet().getSubject();

                    User user = userRepository.findByUsername(username).orElse(null);
                    if (user != null) {
                        String userRoom = "user-" + user.getId();
                        client.joinRoom(userRoom);
                        log.info("Client {} (User: {}) đã vào phòng cá nhân: {}", client.getSessionId(), username, userRoom);
                    }
                }
            } catch (Exception e) {
                log.error("Lỗi khi join user room: ", e);
            }
        });
    }

    public void sendMessageToRoom(String room, String eventName, Object data) {
        log.debug("Gửi sự kiện '{}' đến phòng '{}'", eventName, room);
        server.getRoomOperations(room).sendEvent(eventName, data);
    }
}
