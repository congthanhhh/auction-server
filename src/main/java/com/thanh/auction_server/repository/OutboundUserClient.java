package com.thanh.auction_server.repository;

import com.thanh.auction_server.dto.response.OutboundUserResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "outbound-user-client", url = "${spring.security.oauth2.client.provider.google.user-info-uri}")
public interface OutboundUserClient {
    @GetMapping
    OutboundUserResponse getUserInfo(@RequestParam("alt") String alt,
                                     @RequestParam("access_token") String accessToken);
}
