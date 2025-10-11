package com.thanh.auction_server.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class EmailService {

    JavaMailSender javaMailSender;

    @NonFinal
    @Value("${spring.mail.username}")
    String fromEmail;

    @Async
    public void sendOtpEmail(String to, String otp) {
        try {
            MimeMessage mimeMessage = javaMailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, "utf-8");

            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject("Mã OTP xác thực tài khoản Sàn đấu giá");

            String htmlContent = buildHtmlEmail(otp);
            helper.setText(htmlContent, true);

            javaMailSender.send(mimeMessage);
            log.info("HTML OTP email sent successfully to {}", to);
        } catch (MessagingException e) {
            log.error("Error while sending HTML OTP email to {}: {}", to, e.getMessage());
        }
    }

    private String buildHtmlEmail(String otp) {
        return "<!DOCTYPE html>"
                + "<html lang='vi'>"
                + "<head>"
                + "<style>"
                + "body { font-family: Arial, sans-serif; background-color: #f4f4f4; margin: 0; padding: 0; }"
                + ".container { max-width: 600px; margin: 20px auto; background-color: #ffffff; padding: 20px; box-shadow: 0 4px 8px rgba(0, 0, 0, 0.1); border-radius: 8px;  }"
                + ".header { background-color: #007bff; color: #ffffff; padding: 10px 20px; text-align: center; border-radius: 8px 8px 0 0; }"
                + ".header h1 { margin: 0; }"
                + ".content { padding: 30px; text-align: center; color: #333333; }"
                + ".content p { font-size: 16px; line-height: 1.5; }"
                + ".otp-code { font-size: 36px; font-weight: bold; color: #007bff; margin: 20px 0; letter-spacing: 5px; padding: 10px; background-color: #e9ecef; border-radius: 5px; display: inline-block; }"
                + ".footer { text-align: center; font-size: 12px; color: #777777; padding-top: 20px; border-top: 1px solid #eeeeee; }"
                + "</style>"
                + "</head>"
                + "<body>"
                + "<div class='container'>"
                + "<div class='header'><h1>Sàn Đấu Giá Trực Tuyến</h1></div>"
                + "<div class='content'>"
                + "<p>Xin chào,</p>"
                + "<p>Cảm ơn bạn đã đăng ký. Vui lòng sử dụng mã OTP dưới đây để xác thực tài khoản của bạn.</p>"
                + "<div class='otp-code'>" + otp + "</div>"
                + "<p>Mã này sẽ hết hạn trong 5 phút.</p>"
                + "<p>Nếu bạn không yêu cầu mã này, vui lòng bỏ qua email này.</p>"
                + "</div>"
                + "<div class='footer'>"
                + "<p>&copy; 2025 Sàn Đấu Giá Trực Tuyến. Mọi quyền được bảo lưu.</p>"
                + "</div>"
                + "</div>"
                + "</body>"
                + "</html>";
    }
}