package com.dms.rescueService.rescue;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class RescueApplication {

	public static void main(String[] args) {
		SpringApplication.run(RescueApplication.class, args);
	}

}
