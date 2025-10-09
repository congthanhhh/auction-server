package com.thanh.auction_server.constants;

public final class Endpoints {

    public static final String[] PUBLIC_GET_ENDPOINTS = {
            "/categories/**",
            "/products/**",
            "/bids/**",
            "/auctions/**",
    };

    public static final String[] PUBLIC_POST_ENDPOINTS = {
            "/auth/outbound/authenticate",
            "/auth/authenticate",
            "/auth/refresh-token",
            "/users",
            "/categories/**",
            "/products/**",
            "/bids/**",
            "/auctions/**",

    };
}
