package com.example.consumer_app;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class ConsumerAppApplication {

	public static void main(String[] args) {
		SpringApplication.run(ConsumerAppApplication.class, args);
		System.out.println("=".repeat(80));
		System.out.println("🐰 Consumer App - Started!");
		System.out.println("   Listening for video upload events...");
		System.out.println("=".repeat(80));
	}

}
