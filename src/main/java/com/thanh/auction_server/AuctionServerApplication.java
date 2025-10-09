package com.thanh.auction_server;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients
public class AuctionServerApplication {

	public static void main(String[] args) {
		SpringApplication.run(AuctionServerApplication.class, args);
	}

}
