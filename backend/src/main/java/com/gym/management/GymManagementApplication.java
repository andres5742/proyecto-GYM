package com.gym.management;

import com.gym.management.config.JwtProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableConfigurationProperties(JwtProperties.class)
@EnableScheduling
public class GymManagementApplication {

	public static void main(String[] args) {
		SpringApplication.run(GymManagementApplication.class, args);
	}

}
