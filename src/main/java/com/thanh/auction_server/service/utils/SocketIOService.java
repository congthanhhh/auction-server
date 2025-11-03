package com.thanh.auction_server.service.utils;

import com.corundumstudio.socketio.SocketIOServer;
import jakarta.annotation.PostConstruct;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class SocketIOService {
    private final SocketIOServer server;

    public static final String EVENT_JOIN_SESSION_ROOM = "join_auction_session"; // Client tham gia xem phiên
    public static final String EVENT_LEAVE_SESSION_ROOM = "leave_auction_session"; // Client rời khỏi xem phiên
    public static final String EVENT_JOIN_USER_ROOM = "join_user_room"; // Client tham gia phòng thông báo cá nhân (cho bell icon)
    // Server gửi xuống
    public static final String EVENT_PRICE_UPDATE = "price_update"; // Cập nhật giá & người thắng
    public static final String EVENT_NEW_BID = "new_bid"; // Gửi lịch sử bid mới
    public static final String EVENT_AUCTION_ENDED = "auction_ended"; // Thông báo phiên kết thúc
    public static final String EVENT_STATUS_CHANGE = "status_change"; // Thông báo trạng thái (VD: ACTIVE)
    public static final String EVENT_NEW_NOTIFICATION = "new_notification"; // Ping cho "bell icon"

    @PostConstruct
    private void addEventListeners() {

        /**
         * Lắng nghe sự kiện khi Client muốn "Join" (tham gia) một "Room" (phòng).
         * "Room" là cách Socket.IO gửi tin nhắn cho một nhóm client cụ thể (những người đang xem cùng 1 phiên).
         * Frontend (React) sẽ gửi: socket.emit("join_auction_session", "session-123");
         */
        server.addEventListener(EVENT_JOIN_SESSION_ROOM, String.class, (client, roomName, ackRequest) -> {
            client.joinRoom(roomName); // Cho client này vào phòng
            log.info("Client {} đã tham gia phòng (room): {}", client.getSessionId(), roomName);
        });

        /**
         * Lắng nghe sự kiện khi Client muốn "Leave" (rời) một phòng.
         */
        server.addEventListener(EVENT_LEAVE_SESSION_ROOM, String.class, (client, roomName, ackRequest) -> {
            client.leaveRoom(roomName); // Lấy client ra khỏi phòng
            log.info("Client {} đã rời phòng (room): {}", client.getSessionId(), roomName);
        });

        /**
         * Lắng nghe sự kiện khi Client tham gia phòng thông báo cá nhân (cho bell icon)
         * Frontend (React) sẽ gửi: socket.emit("join_user_room", "user-uuid-cuanhanvien");
         */
        server.addEventListener(EVENT_JOIN_USER_ROOM, String.class, (client, roomName, ackRequest) -> {
            client.joinRoom(roomName);
            log.info("Client {} đã tham gia phòng thông báo cá nhân (room): {}", client.getSessionId(), roomName);
        });

        // Bạn có thể thêm các listener khác nếu cần
    }
    public void sendMessageToRoom(String room, String eventName, Object data) {
        log.debug("Đang phát sóng sự kiện '{}' đến phòng '{}'.", eventName, room);

        // Gửi sự kiện 'eventName' với 'data' đến tất cả client trong 'room'
        server.getRoomOperations(room).sendEvent(eventName, data);
    }
}
