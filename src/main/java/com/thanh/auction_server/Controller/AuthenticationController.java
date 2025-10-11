package com.thanh.auction_server.Controller;

import com.nimbusds.jose.JOSEException;
import com.thanh.auction_server.dto.request.AuthenticationRequest;
import com.thanh.auction_server.dto.request.IntrospectRequest;
import com.thanh.auction_server.dto.request.OtpVerificationRequest;
import com.thanh.auction_server.dto.request.RefreshTokenRequest;
import com.thanh.auction_server.dto.response.AuthenticationResponse;
import com.thanh.auction_server.dto.response.IntrospectResponse;
import com.thanh.auction_server.dto.response.MessageResponse;
import com.thanh.auction_server.service.AuthenticationService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.text.ParseException;

@RequiredArgsConstructor
@RestController
@RequestMapping("/auth")
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AuthenticationController {

    AuthenticationService authenticationService;

    @PostMapping("/authenticate")
    ResponseEntity<AuthenticationResponse> authenticate(@RequestBody AuthenticationRequest request) {
        return ResponseEntity.ok(authenticationService.authenticate(request));
    }

    @PostMapping("/introspect")
    ResponseEntity<IntrospectResponse> introspect(@RequestBody IntrospectRequest request)
            throws ParseException, JOSEException {
        return ResponseEntity.ok(authenticationService.introspect(request));
    }

    @PostMapping("/refresh-token")
    ResponseEntity<AuthenticationResponse> refresh(@RequestBody RefreshTokenRequest request) {
        return ResponseEntity.ok(authenticationService.refreshToken(request));
    }

    @PostMapping("/outbound/authenticate")
    ResponseEntity<AuthenticationResponse> outboundAuthenticate(@RequestParam("code") String code) {
        return ResponseEntity.ok(authenticationService.outboundAuthenticate(code));
    }

    @PostMapping("/verify-otp")
    ResponseEntity<MessageResponse> verifyOtp(@RequestBody OtpVerificationRequest request) {
        return ResponseEntity.ok(authenticationService.verifyAccount(request));
    }
}
