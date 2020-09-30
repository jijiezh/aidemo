package com.zhxd.ai;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication(scanBasePackages = {"com.zhxd"})
public class AidemoApplication {

	public static void main(String[] args) {
		SpringApplication.run(AidemoApplication.class, args);
	}

}
