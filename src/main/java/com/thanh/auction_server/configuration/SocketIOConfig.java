package com.thanh.auction_server.configuration;

import com.corundumstudio.socketio.Configuration;
import com.corundumstudio.socketio.SocketIOServer;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class SocketIOConfig {
    @Value("${socket.port}")
    private int socketPort;

    @Value("${socket.host}")
    private String socketHost;

    private SocketIOServer server;

    @Bean
    public SocketIOServer socketIOServer() {
        Configuration config = new Configuration();
        config.setHostname(socketHost);
        config.setPort(socketPort);
        config.setOrigin("http://localhost:5173");
        this.server = new SocketIOServer(config);
        return this.server;
    }

    @Bean
    public CommandLineRunner socketIoServerRunner(SocketIOServer server) {
        return args -> {
            log.info("Starting Socket.IO server at {}:{}", socketHost, socketPort);
            server.start(); // Khởi động server

            // Lắng nghe các sự kiện kết nối/ngắt kết nối cơ bản
            server.addConnectListener(client -> {
                log.info("Client connected: {}", client.getSessionId());
                // Bạn có thể lấy token xác thực ở đây nếu gửi từ client
                // String token = client.getHandshakeData().getSingleUrlParam("token");
            });

            server.addDisconnectListener(client -> {
                log.info("Client disconnected: {}", client.getSessionId());
            });
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
