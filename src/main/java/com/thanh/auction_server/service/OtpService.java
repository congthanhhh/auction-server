package com.thanh.auction_server.service;

import com.thanh.auction_server.entity.User;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class OtpService {
    RedisTemplate<String, String> redisTemplate;

    public String generateAndSaveOtp(User user) {
        String otp = generateOtp();
        String key = "otp:" + user.getId() + ":EMAIL_VERIFICATION";
        redisTemplate.opsForValue().set(key, otp, 5, TimeUnit.MINUTES);
        return otp;
    }

    public boolean verifyOtp(User user, String otp) {
        String key = "otp:" + user.getId() + ":EMAIL_VERIFICATION";
        String storedOtp = redisTemplate.opsForValue().get(key);

        if (storedOtp.equals(otp)) {
            redisTemplate.delete(key);
            return true;
        }
        return false;
    }

    private String generateOtp() {
        SecureRandom random = new SecureRandom();
        int otp = 100000 + random.nextInt(900000);
        return String.valueOf(otp);
    }
}
