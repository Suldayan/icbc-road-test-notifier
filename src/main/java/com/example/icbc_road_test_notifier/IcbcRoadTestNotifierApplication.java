package com.example.icbc_road_test_notifier;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.modulith.ApplicationModule;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@ApplicationModule
@EnableRetry
@EnableAsync
@ConfigurationPropertiesScan
public class IcbcRoadTestNotifierApplication {

	public static void main(String[] args) {
		SpringApplication.run(IcbcRoadTestNotifierApplication.class, args);
	}

}
