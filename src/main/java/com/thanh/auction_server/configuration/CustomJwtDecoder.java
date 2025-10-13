package com.thanh.auction_server.configuration;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jwt.SignedJWT;
import com.thanh.auction_server.dto.request.IntrospectRequest;
import com.thanh.auction_server.service.authenticate.AuthenticationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.stereotype.Component;

import javax.crypto.spec.SecretKeySpec;
import java.text.ParseException;
import java.util.Objects;

@Component
public class CustomJwtDecoder implements JwtDecoder {
    @Value("${jwt.signerKey}")
    private String signerKey;

    @Autowired
    private AuthenticationService authenticationService;

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    private NimbusJwtDecoder nimbusJwtDecoder = null;

    @Override
    public Jwt decode(String token) throws JwtException {
        try {
            SignedJWT signedJWT = SignedJWT.parse(token);
            String jti = signedJWT.getJWTClaimsSet().getJWTID();
            Boolean isLoggedOut = redisTemplate.hasKey("blocklist:" + jti);
            if (isLoggedOut) {
                throw new JwtException("Token has been logged out");
            }
            var response = authenticationService.introspect(IntrospectRequest.builder().token(token).build());
            if (!response.isValid()) throw new JwtException(" -> ('l')Invalid token");
        } catch (JOSEException | ParseException e) {
            throw new JwtException(e.getMessage() + " ->('l')Token is malformed");
        }
        if (Objects.isNull(nimbusJwtDecoder)) {
            SecretKeySpec secretKey = new SecretKeySpec(signerKey.getBytes(), "HS256");
            nimbusJwtDecoder = NimbusJwtDecoder
                    .withSecretKey(secretKey)
                    .macAlgorithm(MacAlgorithm.HS256)
                    .build();
        }
        return nimbusJwtDecoder.decode(token);
    }
}
