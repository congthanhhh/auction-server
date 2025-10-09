package com.thanh.auction_server.repository;


import com.thanh.auction_server.dto.request.ExchangeTokenRequest;
import com.thanh.auction_server.dto.response.ExchangeTokenResponse;
import feign.QueryMap;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;

@FeignClient(name = "outbound-identity", url = "${spring.security.oauth2.client.provider.google.token-uri}")
public interface OutboundIdentityClient {
    @PostMapping(produces = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    ExchangeTokenResponse exchangeToken(@QueryMap ExchangeTokenRequest request);
}
