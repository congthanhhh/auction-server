package com.thanh.auction_server.service;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class EmailService {
    JavaMailSender javaMailSender;

    @Async
    public void sendOtpEmail(String to, String otp) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom("your-email@gmail.com"); // Có thể cấu hình trong application.yaml
            message.setTo(to);
            message.setSubject("Mã OTP xác thực tài khoản Sàn đấu giá");
            message.setText("Xin chào,\n\nMã OTP của bạn là: " + otp + "\n\nMã này sẽ hết hạn sau 5 phút.\n\nTrân trọng.");

            javaMailSender.send(message);
            log.info("OTP email sent successfully to {}", to);
        } catch (Exception e) {
            log.error("Error while sending OTP email to {}: {}", to, e.getMessage());
            throw new RuntimeException("Lỗi khi gửi email OTP");
        }
    }
}
