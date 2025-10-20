package com.thanh.auction_server.constants;

public final class Endpoints {

    public static final String[] PUBLIC_GET_ENDPOINTS = {
            "/categories/**",
            "/products/**",
            "/bids/**",
            "/auctions/**",

//           test api
            "/images/**",
            "/products/**",
    };

    public static final String[] PUBLIC_POST_ENDPOINTS = {
            "/auth/outbound/authenticate",
            "/auth/authenticate",
            "/auth/refresh-token",
            "/auth/verify-otp",
            "/users",
            "/users/forgot-password",
            "/users/reset-password",
            "/users/otp",
            "/categories/**",

            // test api
            "/images/**",
            "/products/**",

    };
}
