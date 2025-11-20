package com.thanh.auction_server;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableFeignClients
@EnableAsync
//@EnableScheduling
public class AuctionServerApplication {

	public static void main(String[] args) {
		SpringApplication.run(AuctionServerApplication.class, args);
	}

}
