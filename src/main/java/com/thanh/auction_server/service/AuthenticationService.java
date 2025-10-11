package com.thanh.auction_server.service;

import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.thanh.auction_server.constants.ErrorMessage;
import com.thanh.auction_server.constants.RoleEnum;
import com.thanh.auction_server.dto.request.*;
import com.thanh.auction_server.dto.response.AuthenticationResponse;
import com.thanh.auction_server.dto.response.IntrospectResponse;
import com.thanh.auction_server.dto.response.MessageResponse;
import com.thanh.auction_server.entity.RefreshToken;
import com.thanh.auction_server.entity.Role;
import com.thanh.auction_server.entity.User;
import com.thanh.auction_server.exception.UnauthorizedException;
import com.thanh.auction_server.exception.UserNotFoundException;
import com.thanh.auction_server.repository.*;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.SignedJWT;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.text.ParseException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;

@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
@Service
public class AuthenticationService {
    UserRepository userRepository;
    RoleRepository roleRepository;
    RefreshTokenRepository refreshTokenRepository;
    OutboundIdentityClient outboundIdentityClient;
    private final OutboundUserClient outboundUserClient;
    OtpService otpService;

    @NonFinal
    @Value("${jwt.valid-duration}")
    protected long VALID_DURATION;

    @NonFinal
    @Value("${jwt.refreshable-duration}")
    protected long REFRESH_DURATION;

    @NonFinal
    @Value("${jwt.signerKey}")
    protected String SIGNER_KEY;

    @NonFinal
    @Value("${spring.security.oauth2.client.registration.google.client-id}")
    protected String CLIENT_ID;

    @NonFinal
    @Value("${spring.security.oauth2.client.registration.google.client-secret}")
    protected String CLIENT_SECRET;

    @NonFinal
    @Value("${spring.security.oauth2.client.registration.google.redirect-uri}")
    protected String REDIRECT_URI;

    @NonFinal
    protected final String GRANT_TYPE = "authorization_code";

    public IntrospectResponse introspect(IntrospectRequest request) throws JOSEException, ParseException {
        var token = request.getToken();
        boolean isValid = true;
        try {
            verifyToken(token);
        } catch (Exception e) {
            isValid = false;
        }
        return IntrospectResponse.builder()
                .valid(isValid)
                .build();
    }

    public AuthenticationResponse outboundAuthenticate(String code) {
        var response = outboundIdentityClient.exchangeToken(ExchangeTokenRequest.builder()
                .code(code)
                .clientId(CLIENT_ID)
                .clientSecret(CLIENT_SECRET)
                .redirectUri(REDIRECT_URI)
                .grantType(GRANT_TYPE)
                .build());
        var userInfo = outboundUserClient.getUserInfo("json", response.getAccessToken());

        var user = userRepository.findByUsername(userInfo.getEmail()).orElseGet(() -> {
            Set<Role> roles = new HashSet<>();
            roleRepository.findById(RoleEnum.USER.name())
                    .ifPresent(roles::add);
            User newUser = User.builder()
                    .username(userInfo.getEmail())
                    .email(userInfo.getEmail())
                    .firstName(userInfo.getGivenName())
                    .lastName(userInfo.getFamilyName())
                    .isActive(true)
                    .createdAt(LocalDateTime.now())
                    .roles(roles)
                    .build();
            return userRepository.save(newUser);
        });

        var accessToken = generateToken(user);
        var refreshToken = createRefreshToken(user);

        return AuthenticationResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken.getToken())
                .authenticated(true)
                .build();
    }

    public AuthenticationResponse authenticate(AuthenticationRequest request) {
        PasswordEncoder passwordEncoder = new BCryptPasswordEncoder(10);
        var user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new UserNotFoundException(ErrorMessage.USER_NOT_FOUND));

        if (!user.getIsActive()) {
            throw new UnauthorizedException(ErrorMessage.UNAUTHORIZED);
        }

        boolean authenticated = passwordEncoder.matches(request.getPassword(), user.getPassword());
        if (!authenticated) throw new UnauthorizedException(ErrorMessage.INVALID_CREDENTIALS);
        String token = generateToken(user);
        RefreshToken refreshToken = createRefreshToken(user);
        return AuthenticationResponse.builder()
                .accessToken(token)
                .refreshToken(refreshToken.getToken())
                .authenticated(true)
                .build();
    }

    public MessageResponse verifyAccount(OtpVerificationRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new UserNotFoundException(ErrorMessage.USER_NOT_FOUND));
        if (user.getIsActive()) {
            return MessageResponse.builder()
                    .message("Account is already verified.")
                    .build();
        }
        boolean isOtpValid = otpService.verifyOtp(user, request.getOtp());

        if (isOtpValid) {
            user.setIsActive(true);
            userRepository.save(user);
            return MessageResponse.builder()
                    .message("Account verified successfully.")
                    .build();
        } else {
            throw new UnauthorizedException(ErrorMessage.INVALID_OTP);
        }
    }

    private RefreshToken createRefreshToken(User user) {
        RefreshToken refreshToken = RefreshToken.builder()
                .token(UUID.randomUUID().toString())
                .user(user)
                .createdAt(Instant.now())
                .expiryDate(Instant.now().plusSeconds(REFRESH_DURATION))
                .revoked(false)
                .build();
        return refreshTokenRepository.save(refreshToken);
    }

    public AuthenticationResponse refreshToken(RefreshTokenRequest request) {
        var oldRefreshToken = request.getRefreshToken();
        var refreshToken = refreshTokenRepository.findByToken(oldRefreshToken)
                .orElseThrow(() -> new UnauthorizedException(ErrorMessage.INVALID_REFRESH_TOKEN));
        if (refreshToken.getExpiryDate().isBefore(Instant.now()) || refreshToken.isRevoked()) {
            refreshTokenRepository.delete(refreshToken);
            throw new UnauthorizedException(ErrorMessage.REFRESH_TOKEN_EXPIRED);
        }
        refreshToken.setRevoked(true);
        User user = refreshToken.getUser();
        String token = generateToken(user);
        RefreshToken newRefreshToken = createRefreshToken(user);
        refreshTokenRepository.save(refreshToken);
        return AuthenticationResponse.builder()
                .accessToken(token)
                .refreshToken(newRefreshToken.getToken())
                .authenticated(true)
                .build();
    }

    private void verifyToken(String token) throws ParseException, JOSEException {
        JWSVerifier verifier = new MACVerifier(SIGNER_KEY.getBytes());
        SignedJWT signedJWT = SignedJWT.parse(token);
        Date expirationTime = signedJWT.getJWTClaimsSet().getExpirationTime();
        var verified = signedJWT.verify(verifier);
        if (!verified || expirationTime.before(new Date()))
            throw new UnauthorizedException(ErrorMessage.INVALID_TOKEN);
    }

    private String generateToken(User user) {
        JWSHeader header = new JWSHeader(JWSAlgorithm.HS256);
        JWTClaimsSet jwtClaimsSet = new JWTClaimsSet.Builder()
                .subject(user.getUsername())
                .issuer("auction.com")
                .issueTime(new Date())
                .expirationTime(new Date(
                        Instant.now().plus(VALID_DURATION, ChronoUnit.SECONDS).toEpochMilli()
                ))
                .jwtID(UUID.randomUUID().toString())
                .claim("scope", buildScope(user))
                .build();
        Payload payload = new Payload(jwtClaimsSet.toJSONObject());
        JWSObject jwsObject = new JWSObject(header, payload);
        try {
            jwsObject.sign(new MACSigner(SIGNER_KEY.getBytes()));
            return jwsObject.serialize();
        } catch (JOSEException e) {
            log.error("Cannot create token", e);
            throw new RuntimeException(e);
        }
    }

    private String buildScope(User user) {
        StringJoiner stringJoiner = new StringJoiner(" ");
        if (!CollectionUtils.isEmpty(user.getRoles()))
            user.getRoles().forEach(role -> {
                stringJoiner.add("ROLE_" + role.getName());
                if (!CollectionUtils.isEmpty(role.getPermissions()))
                    role.getPermissions().forEach(permission -> stringJoiner.add(permission.getName()));
            });
        return stringJoiner.toString();
    }

}
