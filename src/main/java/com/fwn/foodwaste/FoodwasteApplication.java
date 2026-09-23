package com.fwn.foodwaste;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.TimeZone;

@SpringBootApplication
public class FoodwasteApplication {

	public static void main(String[] args) {
		TimeZone.setDefault(TimeZone.getTimeZone("Asia/Kathmandu")); // and this line added
		SpringApplication.run(FoodwasteApplication.class, args);
	}

}
