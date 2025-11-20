package com.thanh.auction_server.configuration;

import com.corundumstudio.socketio.AuthorizationResult;
import com.corundumstudio.socketio.SocketIOServer;
import com.corundumstudio.socketio.protocol.JacksonJsonSupport;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.thanh.auction_server.dto.request.IntrospectRequest;
import com.thanh.auction_server.service.authenticate.AuthenticationService;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

@Configuration
@Slf4j
@RequiredArgsConstructor
public class SocketIOConfig {
    @Value("${socket.port}")
    private int socketPort;

    @Value("${socket.host}")
    private String socketHost;

    private SocketIOServer server;
    private final AuthenticationService authenticationService;

    @Bean
    public SocketIOServer socketIOServer() {
        com.corundumstudio.socketio.Configuration config = new com.corundumstudio.socketio.Configuration();
        config.setHostname(socketHost);
        config.setPort(socketPort);
        config.setOrigin("http://localhost:5173");
        config.setJsonSupport(new JacksonJsonSupport(new JavaTimeModule())); // Date/Time API

        config.setAuthorizationListener(data -> {
            try {
                // Client React SẼ PHẢI gửi token qua handshake query
                // Ví dụ: io("...", { auth: { token: "..." } })
                // netty-socketio sẽ đọc nó qua "getSingleUrlParam"
                String token = data.getSingleUrlParam("token");

                if (token == null) {
                    log.warn("Socket.IO: Client kết nối không có token.");
                    return new AuthorizationResult(false); // Từ chối kết nối
                }

                // Tái sử dụng logic introspect của bạn để kiểm tra token
                var response = authenticationService.introspect(
                        IntrospectRequest.builder().token(token).build());

                if (response.isValid()) {
                    // Token hợp lệ, cho phép kết nối
                    log.info("Socket.IO: Client đã xác thực thành công (token hợp lệ).");
                    return new AuthorizationResult(true);
                } else {
                    log.warn("Socket.IO: Client bị từ chối, token không hợp lệ.");
                    return new AuthorizationResult(false); // Từ chối kết nối
                }
            } catch (Exception e) {
                log.error("Socket.IO: Lỗi xác thực token: {}", e.getMessage());
                return new AuthorizationResult(false); // Từ chối nếu có lỗi
            }
        });

        this.server = new SocketIOServer(config);
        return this.server;
    }

    @Bean
    public CommandLineRunner socketIoServerRunner(SocketIOServer server) {
        return args -> {
            log.info("Starting Socket.IO server at {}:{}", socketHost, socketPort);
            server.start();
        };
    }

    @PreDestroy
    public void stopServer() {
        if (this.server != null) {
            log.info("Stopping Socket.IO server...");
            this.server.stop();
        }
    }
}
