package com.thanh.auction_server.Controller;

import com.nimbusds.jose.JOSEException;
import com.thanh.auction_server.dto.request.*;
import com.thanh.auction_server.dto.response.AuthenticationResponse;
import com.thanh.auction_server.dto.response.IntrospectResponse;
import com.thanh.auction_server.dto.response.MessageResponse;
import com.thanh.auction_server.exception.UnauthorizedException;
import com.thanh.auction_server.service.authenticate.AuthenticationService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.text.ParseException;

@RequiredArgsConstructor
@RestController
@RequestMapping("/auth")
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AuthenticationController {

    AuthenticationService authenticationService;

    @PostMapping("/authenticate")
    ResponseEntity<AuthenticationResponse> authenticate(@RequestBody AuthenticationRequest request,
                                                        HttpServletResponse response) {
        var authResponse = authenticationService.authenticate(request);
        ResponseCookie refreshCookie = createRefreshTokenCookie(authResponse.getRefreshToken());
        response.addHeader(HttpHeaders.SET_COOKIE, refreshCookie.toString());
        authResponse.setRefreshToken(null);
        return ResponseEntity.ok(authResponse);
    }

    @PostMapping("/introspect")
    ResponseEntity<IntrospectResponse> introspect(@RequestBody IntrospectRequest request)
            throws ParseException, JOSEException {
        return ResponseEntity.ok(authenticationService.introspect(request));
    }

    @PostMapping("/refresh-token")
    ResponseEntity<AuthenticationResponse> refresh(
            @CookieValue(name = "refresh_token", required = false) String refreshToken,
            HttpServletResponse response
    ) {
        if (refreshToken == null) {
            throw new UnauthorizedException("Refresh Token is missing in Cookie");
        }
        var authResponse = authenticationService.refreshToken(refreshToken);
        // Cập nhật lại Cookie mới (vì refreshToken xoay vòng)
        ResponseCookie newRefreshCookie = createRefreshTokenCookie(authResponse.getRefreshToken());
        response.addHeader(HttpHeaders.SET_COOKIE, newRefreshCookie.toString());

        authResponse.setRefreshToken(null);

        return ResponseEntity.ok(authResponse);
    }

    @PostMapping("/outbound/authenticate")
    ResponseEntity<AuthenticationResponse> outboundAuthenticate(@RequestParam("code") String code,
                                                                HttpServletResponse response) {
        var authResponse = authenticationService.outboundAuthenticate(code);

        ResponseCookie refreshCookie = createRefreshTokenCookie(authResponse.getRefreshToken());
        response.addHeader(HttpHeaders.SET_COOKIE, refreshCookie.toString());

        authResponse.setRefreshToken(null);

        return ResponseEntity.ok(authResponse);
    }

    @PostMapping("/verify-otp")
    ResponseEntity<MessageResponse> verifyOtp(@RequestBody OtpVerificationRequest request) {
        return ResponseEntity.ok(authenticationService.verifyAccount(request));
    }

    @PostMapping("/logout")
    ResponseEntity<MessageResponse> logout(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authHeader,
            @CookieValue(name = "refresh_token", required = false) String refreshToken,
            HttpServletResponse response
    ) throws ParseException {

        if (authHeader != null && authHeader.startsWith("Bearer ") && refreshToken != null) {
            String accessToken = authHeader.substring(7);
            LogoutRequest logoutRequest = LogoutRequest.builder()
                    .accessToken(accessToken)
                    .refreshToken(refreshToken)
                    .build();

            authenticationService.logout(logoutRequest);
        }

        ResponseCookie cleanCookie = ResponseCookie.from("refresh_token", "")
                .httpOnly(true)
                .secure(false) // Localhost để false, Deploy production phải để true
                .path("/")
                .maxAge(0) // Xóa ngay lập tức
                .sameSite("Lax")
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, cleanCookie.toString());

        return ResponseEntity.ok(MessageResponse.builder()
                .message("Logged out successful and cookie cleared.")
                .build());
    }

    private ResponseCookie createRefreshTokenCookie(String token) {
        return ResponseCookie.from("refresh_token", token) // Gán chuỗi token vào tên "refresh_token"
                .httpOnly(true)  // Bảo mật
                .path("/")
                .maxAge(604800)
                .build();
    }
}
